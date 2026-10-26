#!/usr/bin/env python3
"""Draws the website's share image and home-screen icon from the app's own logo and home screenshot (D-058).

Run after the logo or the home screenshot changes; the PNGs it writes are committed in site/assets/.
Needs Pillow (python3 -m pip install pillow); the site build itself does not.

  python3 scripts/site-images.py
"""

import json
import re
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SITE = ROOT / "site"
BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
FRAME = (58, 58, 58)


def logo_dots():
    """The wordmark's dots (x, y, r), from docs/brand/zeroos-logo.svg, moved to start at 0, 0."""
    svg = (ROOT / "docs" / "brand" / "zeroos-logo.svg").read_text(encoding="utf-8")
    dots = [tuple(map(float, d)) for d in re.findall(r'cx="([\d.]+)" cy="([\d.]+)" r="([\d.]+)"', svg)]
    left = min(x - r for x, _, r in dots)
    top = min(y - r for _, y, r in dots)
    return [(x - left, y - top, r) for x, y, r in dots]


def draw_logo(image, dots, left, top, width):
    span = max(x + r for x, _, r in dots)
    scale = width / span
    draw = ImageDraw.Draw(image)
    for x, y, r in dots:
        cx, cy, cr = left + x * scale, top + y * scale, r * scale
        draw.ellipse((cx - cr, cy - cr, cx + cr, cy + cr), fill=WHITE)


def share_image(dots, home):
    """1200 x 630 for link previews: the wordmark on the left, the home screen in a phone frame on the right."""
    image = Image.new("RGB", (1200, 630), BLACK)
    height = 560 * max(y + r for _, y, r in dots) / max(x + r for x, _, r in dots)
    draw_logo(image, dots, 90, (630 - height) / 2, 560)
    shot = Image.open(home).convert("RGB")
    shot = shot.resize((round(shot.width * 560 / shot.height), 560), Image.LANCZOS)
    left, top = 1200 - 90 - shot.width, 35
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((left - 8, top - 8, left + shot.width + 8, top + shot.height + 8), radius=36, outline=FRAME, width=2)
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, shot.width, shot.height), radius=28, fill=255)
    image.paste(shot, (left, top), mask)
    return image


def touch_icon(dots):
    """180 x 180 for phones' home screens: the wordmark centred on black."""
    size = 180
    image = Image.new("RGB", (size, size), BLACK)
    span_w = max(x + r for x, _, r in dots)
    span_h = max(y + r for _, y, r in dots)
    width = size * 0.8
    draw_logo(image, dots, (size - width) / 2, (size - span_h * width / span_w) / 2, width)
    return image


def main():
    config = json.loads((SITE / "config.json").read_text(encoding="utf-8"))
    home = ROOT / next(s["src"] for s in config["screens"] if s["key"] == "home")
    dots = logo_dots()
    out = SITE / "assets"
    out.mkdir(exist_ok=True)
    share_image(dots, home).save(out / "og-image.png", optimize=True)
    touch_icon(dots).save(out / "apple-touch-icon.png", optimize=True)
    print(f"site images written to {out}")


if __name__ == "__main__":
    main()
