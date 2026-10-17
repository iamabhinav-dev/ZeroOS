#!/usr/bin/env python3
"""Turn the wallpaper images in docs/brand/wallpapers/ into the app's built-in gallery (F6.8, D-038).

Input:  docs/brand/wallpapers/wallpaper_<name>_<dark|light>.png (or .jpg), portrait, ideally 1260x2800.
Output: app/src/main/assets/wallpapers/<name>_<dark|light>.<webp|dots>, which the app finds on its own.

Two formats (pick with --format):
  webp  the image itself, compressed (default).
  dots  every dot found in the image, stored as a circle (centre and radius); the app draws them fresh at the
        phone's exact resolution, so they stay sharp. Needs numpy and scipy as well.

Both folders are git-ignored: the images are generated from docs/brand/wallpaper-prompts.md, not committed.
Run again after adding or removing images; outputs without a source are deleted.
Needs Python 3 with Pillow (pip install pillow).
"""
import argparse
import re
import struct
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "docs/brand/wallpapers"
TARGET = ROOT / "app/src/main/assets/wallpapers"
STRINGS = ROOT / "core/resources/src/main/res/values/strings.xml"
NAME = re.compile(r"^wallpaper_([a-z0-9_]+)_(dark|light)\.(png|jpe?g)$")
MAX_SIZE = (1260, 2800)          # the phone's screen; larger images are scaled down (never up) here
QUALITY = 85                     # WebP quality: dots stay crisp, files stay small
WARN_TOTAL_MB = 6.0              # NFR-P6: the release APK (about 1.6 MB without wallpapers) must stay under 10 MB


DOTS_MAGIC = b"ZDOT"
DOTS_VERSION = 1
DOTS_UNIT = 8                    # positions and radii are stored in 1/8 pixel


def find_dots(image: Image.Image, dark: bool):
    """Centre (x, y) and radius of every dot, in source pixels. Touching dots are filled with several circles."""
    import numpy as np
    from scipy import ndimage

    grey = np.asarray(image.convert("L"), dtype=np.float32) / 255.0
    ink = grey if dark else 1.0 - grey
    mask = ink > 0.5
    labels, _ = ndimage.label(mask)
    dots = []
    for index, box in enumerate(ndimage.find_objects(labels), start=1):
        if box is None:
            continue
        # Look one pixel around the dot so its soft edge counts towards its size.
        y0, y1 = max(box[0].start - 1, 0), min(box[0].stop + 1, mask.shape[0])
        x0, x1 = max(box[1].start - 1, 0), min(box[1].stop + 1, mask.shape[1])
        crop = labels[y0:y1, x0:x1]
        own = crop == index
        # Only this dot's own soft edge, not the edges of neighbouring dots.
        near = ndimage.binary_dilation(own) & ((crop == 0) | own)
        weight = ink[y0:y1, x0:x1] * near
        area = float(weight.sum())
        if area < 0.5:
            continue
        h, w = y1 - y0 - 2, x1 - x0 - 2
        round_enough = max(w, h) <= 1.35 * max(min(w, h), 1) and own.sum() >= 0.6 * np.pi * (max(w, h) / 2) ** 2
        if round_enough or max(w, h) <= 3:
            ys, xs = np.mgrid[y0:y1, x0:x1]
            dots.append((float((xs * weight).sum() / area) + 0.5, float((ys * weight).sum() / area) + 0.5, float(np.sqrt(area / np.pi))))
            continue
        # Merged dots: place circles at the thickest points until the blob is covered.
        left = own.copy()
        while left.sum() > 2:
            distance = ndimage.distance_transform_edt(left)
            py, px = np.unravel_index(int(distance.argmax()), distance.shape)
            radius = float(distance[py, px])
            if radius < 0.75:
                break
            dots.append((x0 + px + 0.5, y0 + py + 0.5, radius))
            yy, xx = np.ogrid[: left.shape[0], : left.shape[1]]
            left &= (yy - py) ** 2 + (xx - px) ** 2 > (radius + 0.5) ** 2
    return dots


def save_dots(path: Path, width: int, height: int, dots) -> None:
    with path.open("wb") as out:
        out.write(DOTS_MAGIC + struct.pack("<BBHHI", DOTS_VERSION, 0, width, height, len(dots)))
        for x, y, r in dots:
            out.write(struct.pack("<HHH", round(x * DOTS_UNIT), round(y * DOTS_UNIT), max(1, round(r * DOTS_UNIT))))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--format", choices=["webp", "dots"], default="webp")
    fmt = parser.parse_args().format
    if not SOURCE.is_dir():
        print(f"No {SOURCE.relative_to(ROOT)}: nothing to add. The gallery will show Pure black / Pure white only.")
        return 0
    TARGET.mkdir(parents=True, exist_ok=True)
    strings = STRINGS.read_text(encoding="utf-8")
    made, names, skipped = set(), set(), []
    total = 0
    for src in sorted(SOURCE.iterdir()):
        match = NAME.match(src.name)
        if not match:
            if src.is_file():
                skipped.append(src.name)
            continue
        name, tone = match.group(1), match.group(2)
        image = Image.open(src).convert("RGB")
        if image.width > MAX_SIZE[0] or image.height > MAX_SIZE[1]:
            image.thumbnail(MAX_SIZE, Image.LANCZOS)
        out = TARGET / f"{name}_{tone}.{fmt}"
        if fmt == "webp":
            image.save(out, "WEBP", quality=QUALITY, method=6)
        else:
            dots = find_dots(image, dark=tone == "dark")
            save_dots(out, image.width, image.height, dots)
        size = out.stat().st_size
        total += size
        made.add(out.name)
        names.add(name)
        print(f"  {out.name:36} {image.width}x{image.height}  {size / 1024:6.0f} KB")
    for old in [*TARGET.glob("*.webp"), *TARGET.glob("*.dots")]:
        if old.name not in made:
            old.unlink()
            print(f"  removed {old.name}")
    print(f"{len(made)} wallpapers, {total / 1024 / 1024:.2f} MB added to the APK")
    if total / 1024 / 1024 > WARN_TOTAL_MB:
        print(f"WARNING: more than {WARN_TOTAL_MB} MB of wallpapers; the APK may exceed the 10 MB limit (NFR-P6).")
    missing = sorted(n for n in names if f'name="wallpaper_{n}"' not in strings)
    if missing:
        print("Add a label for each of these to core/resources/src/main/res/values/strings.xml:")
        for n in missing:
            print(f'  <string name="wallpaper_{n}" tools:ignore="UnusedResources">{n.replace("_", " ").capitalize()}</string>')
    if skipped:
        print("Ignored (name must be wallpaper_<name>_<dark|light>.png or .jpg): " + ", ".join(skipped))
    return 0


if __name__ == "__main__":
    sys.exit(main())
