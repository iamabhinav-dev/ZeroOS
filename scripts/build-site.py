#!/usr/bin/env python3
"""Builds the ZeroOS website from site/ into build/site/ and fails on hardcoded text.

All user-facing text lives in site/strings/en.json. index.html only names keys:
  data-t="key"                 the element's text
  data-t-attr="alt:key,..."    attribute values
  data-href="configKey"        a link from site/config.json
  <!-- @block -->              generated from config.json (screens, features, docs, channels, logo, data)
Screenshots, the Doto font and the logo are copied from the app's own files, so the site follows the app.

  python3 scripts/build-site.py            build into build/site/
  python3 scripts/build-site.py --check    only check, write nothing
"""

import argparse
import html
import json
import re
import shutil
import sys
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SITE = ROOT / "site"

# Attributes that hold text people read or hear; they must come from strings via data-t-attr.
TEXT_ATTRIBUTES = {"alt", "title", "aria-label", "aria-description", "placeholder"}
KEY = re.compile(r"^[a-z][a-z0-9_]*$")


class SiteError(Exception):
    pass


class TextFinder(HTMLParser):
    """Collects text and text attributes written straight into the HTML template."""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.problems = []
        self.keys = set()
        self._skip = 0

    def handle_starttag(self, tag, attrs):
        if tag in ("script", "style"):
            self._skip += 1
        values = dict(attrs)
        for name, value in attrs:
            if name in TEXT_ATTRIBUTES and value:
                self.problems.append(f"line {self.getpos()[0]}: {name}=\"{value}\" is hardcoded; use data-t-attr")
        if values.get("data-t"):
            self.keys.add(values["data-t"])
        for pair in filter(None, (values.get("data-t-attr") or "").split(",")):
            self.keys.add(pair.split(":", 1)[1].strip())

    def handle_endtag(self, tag):
        if tag in ("script", "style"):
            self._skip -= 1

    def handle_data(self, data):
        if not self._skip and data.strip():
            self.problems.append(f"line {self.getpos()[0]}: text \"{data.strip()}\" is hardcoded; use data-t")


JS_DIRECTIVES = {"use strict"}
LITERAL = re.compile(r"([\"'])((?:\\.|(?!\1).)*)\1")


def strip_comments(source):
    """Drops /* */ comments and whole-line // comments (URLs inside strings stay)."""
    source = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), source, flags=re.S)
    return re.sub(r"(?m)^\s*//.*$", "", source)


def js_problems(source):
    """String literals in the script that read like words for people, outside error messages."""
    problems = []
    source = strip_comments(source)
    for match in LITERAL.finditer(source):
        before = source[max(0, match.start() - 6):match.start()]
        if "Error(" in before or match.group(2) in JS_DIRECTIVES:
            continue
        if re.search(r"[A-Za-z]{2,}\s+[A-Za-z]{2,}", match.group(2)):
            line = source.count("\n", 0, match.start()) + 1
            problems.append(f"app.js line {line}: \"{match.group(2)}\" looks like hardcoded text; use t()")
    return problems


def css_problems(source):
    problems = []
    for match in re.finditer(r"content:\s*([\"'])(.+?)\1", source):
        problems.append(f"style.css: content \"{match.group(2)}\" is hardcoded text")
    return problems


def generated_keys(config):
    """Keys the generated blocks use, so they're checked too."""
    keys = {"brand_name", "channel_loading"}
    for screen in config["screens"]:
        keys |= {f"screen_{screen['key']}_{part}" for part in ("title", "body", "alt")}
    for feature in config["features"]:
        keys |= {f"feature_{feature}_title", f"feature_{feature}_body"}
    for doc in config["docs"]:
        keys |= {f"doc_{doc['key']}", f"doc_{doc['key']}_body"}
    for channel in config["channels"]:
        keys |= {f"channel_{channel['id']}_title", f"channel_{channel['id']}_body"}
    return keys


def check(template, script, style, strings, config):
    finder = TextFinder()
    finder.feed(template)
    problems = list(finder.problems) + js_problems(script) + css_problems(style)
    called = set(re.findall(r"\bt\(\s*\"([a-z0-9_]+)\"", script))
    # Keys picked in code (e.g. a ternary) count as used when they name a string.
    named = {literal for _, literal in LITERAL.findall(strip_comments(script)) if literal in strings}
    used = finder.keys | generated_keys(config) | called | named
    problems += [f"missing string: {key}" for key in sorted(used - strings.keys())]
    problems += [f"unused string: {key}" for key in sorted(strings.keys() - used)]
    problems += [f"bad string key: {key}" for key in strings if not KEY.match(key)]
    if problems:
        raise SiteError("\n".join(problems))


def text(strings, key):
    return html.escape(strings[key], quote=True)


def logo_svg(source, label=None):
    """The dotted wordmark in the page's own colour, cropped to its dots."""
    svg = re.sub(r"<title>.*?</title>|<rect[^>]*/>|<!--.*?-->", "", source, flags=re.S)
    circles = [tuple(map(float, c)) for c in re.findall(r'cx="([\d.]+)" cy="([\d.]+)" r="([\d.]+)"', svg)]
    left = min(x - r for x, _, r in circles)
    top = min(y - r for _, y, r in circles)
    width = max(x + r for x, _, r in circles) - left
    height = max(y + r for _, y, r in circles) - top
    svg = re.sub(r"<svg[^>]*>", f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{left:g} {top:g} {width:g} {height:g}"'
                 ' aria-hidden="true" focusable="false">', svg, count=1)
    svg = svg.replace('fill="#FFFFFF"', 'fill="currentColor"')
    return re.sub(r"\s+", " ", svg).strip()


def screen_images(key, alt):
    """The dark and light screenshot of a screen; CSS shows the one matching the page's theme (lazy, so only it loads)."""
    return "".join(
        f'<img class="only-{theme}" src="assets/screens/{key}{suffix}.png" width="1176" height="2616" loading="lazy" alt="{alt}">'
        for theme, suffix in (("dark", ""), ("light", "-light"))
    )


def blocks(strings, config, logo):
    screens = "\n".join(
        f'<figure class="shot"><div class="phone">{screen_images(s["key"], text(strings, "screen_" + s["key"] + "_alt"))}</div>'
        f'<figcaption><strong>{text(strings, "screen_" + s["key"] + "_title")}</strong>'
        f'<span>{text(strings, "screen_" + s["key"] + "_body")}</span></figcaption></figure>'
        for s in config["screens"]
    )
    features = "\n".join(
        f'<li class="feature"><h3>{text(strings, "feature_" + f + "_title")}</h3>'
        f'<p>{text(strings, "feature_" + f + "_body")}</p></li>'
        for f in config["features"]
    )
    docs = "\n".join(
        f'<li><a href="{html.escape(config["docsBase"] + d["path"])}">{text(strings, "doc_" + d["key"])}'
        f'<span>{text(strings, "doc_" + d["key"] + "_body")}</span></a></li>'
        for d in config["docs"]
    )
    doclinks = "\n".join(
        f'<li><a class="link" href="{html.escape(config["docsBase"] + d["path"])}">{text(strings, "doc_" + d["key"])}</a></li>'
        for d in config["docs"] if d["key"] != "changelog"
    )
    channels = "\n".join(
        f'<article class="channel" data-channel="{c["id"]}" data-state="loading">'
        f'<h3>{text(strings, "channel_" + c["id"] + "_title")}</h3>'
        f'<p class="channel-body">{text(strings, "channel_" + c["id"] + "_body")}</p>'
        f'<p class="channel-version" data-role="version"></p>'
        f'<p class="channel-meta" data-role="meta" aria-live="polite">{text(strings, "channel_loading")}</p>'
        f'<div class="channel-actions" data-role="actions"></div></article>'
        for c in config["channels"]
    )
    script_config = {k: config[k] for k in ("releasesApi", "apkSuffix", "channels")}
    payload = json.dumps({"strings": strings, "config": script_config}, ensure_ascii=False).replace("</", "<\\/")
    return {
        "screens": screens,
        "features": features,
        "docs": docs,
        "doclinks": doclinks,
        "channels": channels,
        "logo": logo,
        "data": f'<script id="site-data" type="application/json">{payload}</script>',
    }


def render(template, strings, config, logo):
    def fill_text(match):
        return f"{match.group(1)}{text(strings, match.group(3))}{match.group(4)}"

    out = re.sub(r'(<(\w+)\b[^>]*?\sdata-t="([a-z0-9_]+)"[^>]*>)\s*(</\2>)', fill_text, template)

    def fill_attrs(match):
        pairs = [pair.split(":", 1) for pair in match.group(1).split(",")]
        return " ".join(f'{name.strip()}="{text(strings, key.strip())}"' for name, key in pairs)

    out = re.sub(r'data-t-attr="([^"]+)"', fill_attrs, out)
    out = re.sub(r'\sdata-t="[a-z0-9_]+"', "", out)
    out = re.sub(r'data-href="(\w+)"', lambda m: f'href="{html.escape(config[m.group(1)])}"', out)
    for name, value in blocks(strings, config, logo).items():
        out = out.replace(f"<!-- @{name} -->", value)
    left = re.findall(r"<!-- @\w+ -->", out)
    if left:
        raise SiteError(f"unknown blocks: {left}")
    return out


def build(out_dir, only_check=False):
    template = (SITE / "index.html").read_text(encoding="utf-8")
    script = (SITE / "app.js").read_text(encoding="utf-8")
    style = (SITE / "style.css").read_text(encoding="utf-8")
    strings = json.loads((SITE / "strings" / "en.json").read_text(encoding="utf-8"))
    config = json.loads((SITE / "config.json").read_text(encoding="utf-8"))
    check(template, script, style, strings, config)
    sources = [ROOT / s[v] for s in config["screens"] for v in ("src", "light")] + [ROOT / p for p in config["fonts"].values()] + [ROOT / config["logo"]]
    missing = [str(p.relative_to(ROOT)) for p in sources if not p.is_file()]
    if missing:
        raise SiteError("missing files: " + ", ".join(missing))
    logo_source = (ROOT / config["logo"]).read_text(encoding="utf-8")
    page = render(template, strings, config, logo_svg(logo_source))
    if only_check:
        return
    if out_dir.exists():
        shutil.rmtree(out_dir)
    (out_dir / "assets" / "screens").mkdir(parents=True)
    (out_dir / "assets" / "fonts").mkdir()
    (out_dir / "index.html").write_text(page, encoding="utf-8")
    shutil.copy(SITE / "style.css", out_dir / "style.css")
    shutil.copy(SITE / "app.js", out_dir / "app.js")
    shutil.copy(ROOT / config["logo"], out_dir / "assets" / "icon.svg")
    for path in config["fonts"].values():
        shutil.copy(ROOT / path, out_dir / "assets" / "fonts" / Path(path).name)
    for screen in config["screens"]:
        shutil.copy(ROOT / screen["src"], out_dir / "assets" / "screens" / f"{screen['key']}.png")
        shutil.copy(ROOT / screen["light"], out_dir / "assets" / "screens" / f"{screen['key']}-light.png")
    (out_dir / ".nojekyll").write_text("", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", type=Path, default=ROOT / "build" / "site")
    parser.add_argument("--check", action="store_true", help="only check for hardcoded or missing text")
    args = parser.parse_args()
    try:
        build(args.out, args.check)
    except SiteError as error:
        print(f"site: {error}", file=sys.stderr)
        return 1
    print("site: ok" if args.check else f"site: built into {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
