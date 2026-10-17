"""Copy the brand and mascot PNGs of coffer-docs into Android resources as WebP.

Source of truth: ../docs/08-ui/icons/{brand,mascot}/ (fixed file names, 1024 x 1024, transparent).
Target: app/src/main/res/drawable-nodpi/<name with underscores>.webp, referenced only as R.drawable.*.
Needs Pillow with WebP support. Run it again after the PO replaces an image, then open a PR.
"""
import sys
from pathlib import Path

from PIL import Image

APP = Path(__file__).resolve().parent.parent
ICONS = APP.parent / "docs" / "08-ui" / "icons"
OUT = APP / "app" / "src" / "main" / "res" / "drawable-nodpi"

FILES = [
    "brand/coffer-mark.png",
    "brand/coffer-mark-dark.png",
    "brand/coffer-mark-mono.png",
    *(f"mascot/mascot-{state}.png" for state in
      ("normal", "good", "goal", "attentive", "worried", "debt", "resting", "welcome")),
]
CANVAS = 1024
# Largest on-screen use is ~180 dp; 512 px stays sharp up to xxhdpi and keeps the APK small
SIZE = 512


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    errors = []
    for rel in FILES:
        src = ICONS / rel
        img = Image.open(src)
        # Contract of icons/README.md; a broken image must be normalized first, not silently resized
        if img.size != (CANVAS, CANVAS) or img.mode != "RGBA":
            errors.append(f"{rel}: {img.size} {img.mode}, expected ({CANVAS}, {CANVAS}) RGBA")
            continue
        name = Path(rel).stem.replace("-", "_")
        img.resize((SIZE, SIZE), Image.LANCZOS).save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)
        print(f"{rel} -> drawable-nodpi/{name}.webp")
    for e in errors:
        print("ERROR", e, file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
