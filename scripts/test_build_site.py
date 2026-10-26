#!/usr/bin/env python3
"""Tests for build-site.py; run by `./gradlew check` (task checkSite)."""

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("build_site", HERE / "build-site.py")
site = importlib.util.module_from_spec(spec)
spec.loader.exec_module(site)

CONFIG = {
    "screens": [{"key": "home"}],
    "features": ["home"],
    "docs": [{"key": "readme", "path": "README.md"}],
    "channels": [{"id": "stable"}],
    "docsBase": "https://example.invalid/",
    "releasesApi": "https://example.invalid/api",
    "releasesPage": "https://example.invalid/releases",
    "apkSuffix": "-release.apk",
    "siteUrl": "https://example.invalid",
    "applicationCategory": "UtilitiesApplication",
}
STRINGS = {
    key: "Text"
    for key in sorted(site.generated_keys(CONFIG) | {"page_title", "hero_title", "hero_alt"})
}
TEMPLATE = '<title data-t="page_title"></title><h1 data-t="hero_title"></h1><img data-t-attr="alt:hero_alt">'


class CheckTest(unittest.TestCase):
    def problems(self, template=TEMPLATE, script="", style="", strings=None):
        try:
            site.check(template, script, style, STRINGS if strings is None else strings, CONFIG)
        except site.SiteError as error:
            return str(error)
        return ""

    def test_clean_template_passes(self):
        self.assertEqual("", self.problems())

    def test_hardcoded_text_fails(self):
        self.assertIn("hardcoded", self.problems(TEMPLATE + "<p>Download now</p>"))

    def test_hardcoded_attribute_fails(self):
        self.assertIn('alt="Home screen"', self.problems(TEMPLATE + '<img alt="Home screen">'))

    def test_script_and_style_text_fails(self):
        self.assertIn("app.js line 2", self.problems(script='const a = 1;\nnode.textContent = "Not released yet";'))
        self.assertIn("style.css", self.problems(style='.x::before { content: "New"; }'))

    def test_script_comments_errors_and_directives_pass(self):
        script = '"use strict";\n// Looks like text: don\'t flag it\nthrow new Error("missing string here");'
        self.assertEqual("", self.problems(script=script))

    def test_missing_and_unused_strings_fail(self):
        self.assertIn("missing string: nope", self.problems(script='t("nope");'))
        self.assertIn("unused string: extra", self.problems(strings={**STRINGS, "extra": "Text"}))

    def test_keys_named_in_code_count_as_used(self):
        strings = {**STRINGS, "theme_dark": "Dark", "theme_light": "Light"}
        self.assertEqual("", self.problems(script='t(dark ? "theme_dark" : "theme_light");', strings=strings))


class RenderTest(unittest.TestCase):
    def test_fills_text_attributes_and_blocks(self):
        strings = {**STRINGS, "hero_title": "Less <phone>", "hero_alt": "Home \"screen\""}
        page = site.render(TEMPLATE + "<ul><!-- @features --></ul>", strings, CONFIG, "<svg></svg>")
        self.assertIn("<h1>Less &lt;phone&gt;</h1>", page)
        self.assertIn('alt="Home &quot;screen&quot;"', page)
        self.assertIn('<li class="feature">', page)
        self.assertNotIn("data-t", page)

    def test_unknown_block_fails(self):
        with self.assertRaises(site.SiteError):
            site.render("<!-- @nothing -->", STRINGS, CONFIG, "")

    def test_logo_uses_page_colour_and_is_cropped(self):
        source = ('<svg viewBox="0 0 100 100"><title>Logo</title><rect width="100" height="100" fill="#000000"/>'
                  '<g fill="#FFFFFF"><circle cx="20" cy="30" r="5"/><circle cx="60" cy="40" r="5"/></g></svg>')
        svg = site.logo_svg(source)
        self.assertIn('viewBox="15 25 50 20"', svg)
        self.assertIn('fill="currentColor"', svg)
        self.assertNotIn("<rect", svg)
        self.assertNotIn("<title>", svg)


class SeoTest(unittest.TestCase):
    def test_robots_allows_everything_and_points_to_the_sitemap(self):
        self.assertEqual("User-agent: *\nAllow: /\n\nSitemap: https://example.invalid/sitemap.xml\n", site.robots_txt(CONFIG))

    def test_sitemap_lists_the_page(self):
        xml = site.sitemap_xml(CONFIG, "2026-10-26")
        self.assertIn("<loc>https://example.invalid/</loc><lastmod>2026-10-26</lastmod>", xml)

    def test_seo_block_has_canonical_previews_and_valid_structured_data(self):
        block = site.seo_block({**STRINGS, "page_title": "ZeroOS & co"}, CONFIG)
        self.assertIn('<link rel="canonical" href="https://example.invalid/">', block)
        self.assertIn('<meta property="og:title" content="ZeroOS &amp; co">', block)
        self.assertIn('content="https://example.invalid/assets/og-image.png"', block)
        data = json.loads(block.split('<script type="application/ld+json">')[1].split("</script>")[0])
        self.assertEqual("SoftwareApplication", data["@type"])
        self.assertEqual("0", data["offers"]["price"])
        self.assertNotIn("google-site-verification", block)
        verified = site.seo_block(STRINGS, {**CONFIG, "googleSiteVerification": "abc_123"})
        self.assertIn('<meta name="google-site-verification" content="abc_123">', verified)


class RealSiteTest(unittest.TestCase):
    def test_the_real_site_builds_without_hardcoded_text(self):
        with tempfile.TemporaryDirectory() as out:
            site.build(Path(out) / "site")
            page = (Path(out) / "site" / "index.html").read_text(encoding="utf-8")
            self.assertNotIn("data-t", page)
            self.assertNotIn("<!-- @", page)
            self.assertTrue((Path(out) / "site" / "assets" / "screens" / "home.png").is_file())
            self.assertTrue((Path(out) / "site" / "assets" / "screens" / "home-light.png").is_file())
            self.assertEqual(page.count('class="only-dark"'), page.count('class="only-light"'))
            self.assertTrue((Path(out) / "site" / ".nojekyll").is_file())
            for name in ("robots.txt", "sitemap.xml", "assets/og-image.png", "assets/apple-touch-icon.png"):
                self.assertTrue((Path(out) / "site" / name).is_file(), name)
            self.assertIn('rel="canonical"', page)
            self.assertIn("application/ld+json", page)


if __name__ == "__main__":
    unittest.main()
