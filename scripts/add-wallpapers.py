#!/usr/bin/env python3
"""Turn the wallpaper images in docs/brand/wallpapers/ into the app's built-in gallery (F6.8, D-038).

Input:  docs/brand/wallpapers/wallpaper_<name>_<dark|light>.png (or .jpg), portrait, ideally 1260x2800.
Output: app/src/main/assets/wallpapers/<name>_<dark|light>.webp, which the app finds on its own.

Both folders are git-ignored: the images are generated from docs/brand/wallpaper-prompts.md, not committed.
Run again after adding or removing images; outputs without a source are deleted.
Needs Python 3 with Pillow (pip install pillow).
"""
import re
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


def main() -> int:
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
        out = TARGET / f"{name}_{tone}.webp"
        image.save(out, "WEBP", quality=QUALITY, method=6)
        size = out.stat().st_size
        total += size
        made.add(out.name)
        names.add(name)
        print(f"  {out.name:36} {image.width}x{image.height}  {size / 1024:6.0f} KB")
    for old in TARGET.glob("*.webp"):
        if old.name not in made:
            old.unlink()
            print(f"  removed {old.name} (no source image)")
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
