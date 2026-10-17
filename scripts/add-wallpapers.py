#!/usr/bin/env python3
"""Turn the wallpaper images in docs/brand/wallpapers/ into the app's built-in gallery (F6.8, D-038).

Input:  docs/brand/wallpapers/wallpaper_<name>_<dark|light>.png (or .jpg), portrait, ideally 1260x2800.
Output: app/src/main/assets/wallpapers/<name>_<dark|light>.<webp|dots>, which the app finds on its own.

Two formats (pick with --format):
  dots  every dot found in the image, stored as a circle (centre and radius); the app draws them fresh at the
        phone's exact resolution, so they stay sharp (default, chosen on the phone). Needs numpy and scipy.
  webp  the image itself, compressed: an exact copy, but enlarged on the phone and about 3x bigger.

Both folders are git-ignored: the images are generated from docs/brand/wallpaper-prompts.md, not committed.
Run again after adding or removing images; outputs without a source are deleted.
Needs Python 3 with Pillow, numpy and scipy (pip install pillow numpy scipy).
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
    """Centre (x, y) and radius of every dot, in source pixels.

    A region is taken as one dot only if it's round and the original is lit under almost all of the circle.
    Otherwise (dots touching each other) it's split by looking again with a stricter brightness threshold;
    what still won't split is filled with dots of normal size, so no oversized circle can appear.
    """
    import numpy as np
    from scipy import ndimage

    grey = np.asarray(image.convert("L"), dtype=np.float32) / 255.0
    ink = grey if dark else 1.0 - grey
    height, width = ink.shape
    yy_all, xx_all = np.mgrid[0:height, 0:width]
    dots, leftovers = [], []

    def as_dot(region, y0, x0):
        """(x, y, r) if this region is one clean round dot; () if it's too faint to matter; None to split it."""
        h, w = region.shape
        local = ink[y0:y0 + h, x0:x0 + w]
        # The region already holds the dot's soft edge (lit above 0.3); no neighbouring dots are counted.
        weight = local * region
        area = float(weight.sum())
        if area < 0.4:
            return ()
        ys, xs = yy_all[y0:y0 + h, x0:x0 + w], xx_all[y0:y0 + h, x0:x0 + w]
        cx, cy = float((xs * weight).sum() / area) + 0.5, float((ys * weight).sum() / area) + 0.5
        r = float(np.sqrt(area / np.pi))
        if r <= 1.6:
            return (cx, cy, r)
        inside = (xs + 0.5 - cx) ** 2 + (ys + 0.5 - cy) ** 2 <= (r * 0.85) ** 2
        if not inside.any():
            return None
        core = local[inside]
        # One real dot is evenly solid inside; packed dots show darker gaps between them.
        solid = float(core.mean()) >= 0.85 and float(core.std()) <= 0.08 and float(core.min()) >= 0.6
        round_shape = region.sum() >= 0.7 * np.pi * (max(h, w) / 2 - 1) ** 2
        return (cx, cy, r) if solid and round_shape else None

    def split(region, y0, x0, threshold):
        dot = as_dot(region, y0, x0)
        if dot == ():
            return
        if dot is not None:
            dots.append(dot)
            return
        if threshold > 0.9:
            leftovers.append((region, y0, x0))
            return
        stricter = region & (ink[y0:y0 + region.shape[0], x0:x0 + region.shape[1]] > threshold)
        labels, count = ndimage.label(stricter)
        if count <= 1:
            # Still one piece: look again, stricter.
            split(region, y0, x0, threshold + 0.15)
            return
        for index, box in enumerate(ndimage.find_objects(labels), start=1):
            part = np.zeros_like(region)
            part[box] = labels[box] == index
            # Give each part its own soft edge back (pixels of the region next to it, not of other parts).
            part = ndimage.binary_dilation(part) & region & ((labels == 0) | (labels == index))
            split(part, y0, x0, threshold + 0.15)

    labels, _ = ndimage.label(ink > 0.3)
    for index, box in enumerate(ndimage.find_objects(labels), start=1):
        if box is None:
            continue
        y0, y1 = max(box[0].start - 1, 0), min(box[0].stop + 1, height)
        x0, x1 = max(box[1].start - 1, 0), min(box[1].stop + 1, width)
        split(labels[y0:y1, x0:x1] == index, y0, x0, 0.45)

    # Regions that never split: fill with dots no larger than the image's normal dots.
    radii = np.array([d[2] for d in dots]) if dots else np.array([1.5])
    cap = max(1.5, float(np.percentile(radii, 90)))
    for region, y0, x0 in leftovers:
        left = region & (ink[y0:y0 + region.shape[0], x0:x0 + region.shape[1]] > 0.5)
        yy, xx = np.ogrid[: left.shape[0], : left.shape[1]]
        while left.sum() > 2:
            distance = ndimage.distance_transform_edt(left)
            py, px = np.unravel_index(int(distance.argmax()), distance.shape)
            radius = min(float(distance[py, px]), cap)
            if radius < 0.75:
                break
            dots.append((x0 + px + 0.5, y0 + py + 0.5, radius))
            left &= (yy - py) ** 2 + (xx - px) ** 2 > (radius + 0.6) ** 2
    return dots


def save_dots(path: Path, width: int, height: int, dots) -> None:
    with path.open("wb") as out:
        out.write(DOTS_MAGIC + struct.pack("<BBHHI", DOTS_VERSION, 0, width, height, len(dots)))
        for x, y, r in dots:
            out.write(struct.pack("<HHH", round(x * DOTS_UNIT), round(y * DOTS_UNIT), max(1, round(r * DOTS_UNIT))))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--format", choices=["dots", "webp"], default="dots")
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
