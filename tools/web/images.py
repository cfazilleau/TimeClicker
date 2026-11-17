"""Writes the website's images into docs/img/, from the store graphics (tools/store/compose.py).

    python tools/web/images.py

  img/icon.svg               the launcher icon, rounded (favicon, app bar)
  img/apple-touch-icon.png   180x180
  favicon.ico                16, 32 and 48 px, for browsers and crawlers that ask for /favicon.ico
  img/<lang>/shot-<n>.webp   the phone screenshots, 540x960
  img/<lang>/hero.webp       the screen of the first screenshot, without its caption, rounded and fading out
  img/<lang>/hero-dark.webp the same from the dark-theme screenshot (the last one)
  img/<lang>/og.jpg          the feature graphic, for link previews

Run it after compose.py, then build.py. Needs Pillow.
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
DOCS = ROOT / "docs"
META = ROOT / "fastlane/metadata/android"
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(ROOT / "tools/store"))

from build import DARK_SHOT, SHOTS, WEB  # noqa: E402

# The screen inside a composed phone screenshot (compose.py screenshot_page: 840 px wide at (120, 470),
# corners of 56 px); it runs past the bottom edge.
SCREEN = (120, 470, 960, 1920)
SCREEN_RADIUS = 56
HERO_WIDTH = 760

# The launcher icon (res/drawable/ic_launcher_foreground.xml) on its background colour, with rounded corners.
ICON_SVG = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72">
<rect x="18" y="18" width="72" height="72" rx="20" fill="#2E6B4F"/>
<path d="M54,30 A24,24 0 1,1 33.2,42" stroke="#FFFFFF" stroke-width="5" stroke-linecap="round" fill="none"/>
<path d="M26,37 L40,36 L33,48 Z" fill="#FFFFFF"/>
<path d="M54,40 L54,54 L64,60" stroke="#FFFFFF" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
</svg>
"""


def rounded(im: Image.Image, radius: int) -> Image.Image:
    """[im] with transparent rounded corners."""
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def hero(shot: Path) -> Image.Image:
    """The app's screen from a composed screenshot: rounded top corners, fading out at the bottom."""
    im = Image.open(shot).convert("RGB").crop(SCREEN)
    w, h = im.size
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h + SCREEN_RADIUS), SCREEN_RADIUS, fill=255)
    fade_from = int(h * 0.72)
    fade = Image.linear_gradient("L").rotate(180).resize((w, h - fade_from))  # 255 at the top, 0 at the bottom
    mask.paste(Image.composite(fade, Image.new("L", fade.size, 0), mask.crop((0, fade_from, w, h))), (0, fade_from))
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out.resize((HERO_WIDTH, round(h * HERO_WIDTH / w)), Image.LANCZOS)


def write(im: Image.Image, path: Path, **kw):
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, **kw)
    print(path.relative_to(ROOT))


def main():
    img = DOCS / "img"
    img.mkdir(parents=True, exist_ok=True)
    (img / "icon.svg").write_text(ICON_SVG, encoding="utf-8", newline="\n")
    print((img / "icon.svg").relative_to(ROOT))

    icon = Image.open(META / "en-US/images/icon.png").convert("RGB")
    write(icon.resize((180, 180), Image.LANCZOS), img / "apple-touch-icon.png", optimize=True)
    write(rounded(icon, 112), DOCS / "favicon.ico", sizes=[(16, 16), (32, 32), (48, 48)])

    for locale, (folder, _, _) in WEB.items():
        images = META / locale / "images"
        out = img / folder
        for n in range(1, SHOTS + 1):
            shot = Image.open(images / "phoneScreenshots" / f"{n}.jpg").convert("RGB")
            write(shot.resize((540, 960), Image.LANCZOS), out / f"shot-{n}.webp", quality=80, method=6)
        write(hero(images / "phoneScreenshots/1.jpg"), out / "hero.webp", quality=82, method=6)
        write(hero(images / f"phoneScreenshots/{DARK_SHOT}.jpg"), out / "hero-dark.webp", quality=82, method=6)
        write(Image.open(images / "featureGraphic.png").convert("RGB"), out / "og.jpg", quality=85, optimize=True,
              progressive=True)


if __name__ == "__main__":
    main()
