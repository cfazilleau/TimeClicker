"""Writes the website's landing pages, one per language of the app, into docs/.

    python tools/web/build.py           # docs/index.html (English) and docs/<lang>/index.html
    python tools/web/build.py --check   # fails if those files are out of date

The pages are committed: GitHub Pages serves docs/ as it is, with no build step.
Text: tools/web/strings.json, plus the headline and screenshot captions from tools/store/captions.json and the
undo hint from the app's strings. Icons: the app's Lucide table. Images: tools/web/images.py writes docs/img/.
privacy.html is written by hand (English only).
"""

import argparse
import html
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
DOCS = ROOT / "docs"
sys.path.insert(0, str(ROOT / "tools/store"))

import demo  # noqa: E402
from capture import strings as app_strings  # noqa: E402
from fonts import font  # noqa: E402

SITE = "https://timeclicker.cfaz.dev/"
PLAY = "https://play.google.com/store/apps/details?id=dev.cfaz.timeclicker"
APK = "https://github.com/cfazilleau/TimeClicker/releases/latest"
AUTHOR = "https://github.com/cfazilleau"
LUCIDE = "https://lucide.dev"
PHOTO = "https://commons.wikimedia.org/wiki/File:A_Wet_Leaf.jpg"
PHOTO_LICENCE = "https://creativecommons.org/licenses/by-sa/4.0/"

# Store locale -> (folder under docs/ and docs/img/, HTML lang and hreflang, Google Play badge language).
WEB = {
    "en-US": ("en", "en", "en"), "fr-FR": ("fr", "fr", "fr"), "es-ES": ("es", "es", "es"),
    "de-DE": ("de", "de", "de"), "it-IT": ("it", "it", "it"), "pt-BR": ("pt-br", "pt-BR", "pt-br"),
    "nl-NL": ("nl", "nl", "nl"), "pl-PL": ("pl", "pl", "pl"), "ru-RU": ("ru", "ru", "ru"),
    "tr-TR": ("tr", "tr", "tr"), "id": ("id", "id", "id"), "ar": ("ar", "ar", "ar"),
    "hi-IN": ("hi", "hi", "hi"), "ja-JP": ("ja", "ja", "ja"), "ko-KR": ("ko", "ko", "ko"),
    "zh-CN": ("zh-cn", "zh-CN", "zh-cn"),
}
DEFAULT = "en-US"  # At the site's root; the others in docs/<folder>/.
assert set(WEB) == set(demo.LOCALES)

# The feature tiles, in strings.json order: colour, Lucide icon, wide.
FEATURES = [
    ("sage", "mouse-pointer-click", True), ("lavender", "folders", False), ("peach", "bell", False),
    ("sky", "app-window", False), ("rose", "palette", False), ("teal", "sun-moon", True),
    ("butter", "languages", True), ("slate", "wifi-off", True),
]
SHOTS = 6  # fastlane phoneScreenshots/1..6, as webp in docs/img/<folder>/ (images.py)
SHOT_CAPTIONS = [0, 1, 5, 2, 3, 4]  # Their captions in captions.json: the widgets come third.
DARK_SHOT = 6  # The dark-theme one: the hero in dark mode.


def load_icons() -> dict[str, str]:
    icons = {}
    for line in (ROOT / "app/src/main/assets/lucide/icons.tsv").read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#"):
            name, *_, path = line.split("\t")
            icons[name] = path
    return icons


ICONS = load_icons()


def icon(name: str, cls="i") -> str:
    return f'<svg class="{cls}" viewBox="0 0 24 24" aria-hidden="true"><path d="{ICONS[name]}"/></svg>'


def e(text: str) -> str:
    return html.escape(text, quote=True)


def linked(text: str, urls: list[str]) -> str:
    """The text with each {part} made a link to the next of [urls]."""
    parts = re.split(r"\{([^}]*)\}", text)
    assert len(parts) == 2 * len(urls) + 1, (text, urls)
    out = []
    for i, part in enumerate(parts):
        out.append(e(part) if i % 2 == 0 else f'<a href="{e(urls[i // 2])}">{e(part)}</a>')
    return "".join(out)


def page_url(locale: str) -> str:
    return SITE if locale == DEFAULT else f"{SITE}{WEB[locale][0]}/"


def badge(locale: str, s: dict) -> str:
    code = WEB[locale][2]
    src = f"https://play.google.com/intl/{code}/badges/static/images/badges/{code}_badge_web_generic.png"
    hl = WEB[locale][1]
    return (f'<a class="play-badge" href="{e(PLAY)}&amp;hl={hl}">'
            f'<img alt="{e(s["play_alt"])}" src="{src}" height="64"></a>')


SCRIPT = """
(() => {
  const lang = document.documentElement.lang;

  // The demo tile: ticks like a tile in the app; a tap starts it over, another within 10 seconds undoes it.
  const tile = document.querySelector(".demo");
  if (tile) {
    const when = tile.querySelector(".when"), hint = tile.querySelector(".hint");
    const count = tile.querySelector(".count"), arc = tile.querySelector(".arc circle");
    const hintText = hint.textContent, undoText = tile.dataset.undo;
    const units = [["day", 86400], ["hour", 3600], ["minute", 60], ["second", 1]].map(
      ([unit, s]) => [new Intl.NumberFormat(lang, { style: "unit", unit, unitDisplay: "long" }), s]);
    const number = new Intl.NumberFormat(lang);
    let start = Date.now(), presses = 1, previous = null, undoUntil = 0;
    const draw = () => {
      const now = Date.now(), seconds = Math.floor((now - start) / 1000);
      const [format, s] = units.find(([, s]) => seconds >= s) || units[3];
      const text = format.format(Math.floor(seconds / s));
      if (when.textContent !== text) when.textContent = text;
      const left = Math.max(0, undoUntil - now) / 10000;
      tile.classList.toggle("undoing", left > 0);
      arc.style.strokeDashoffset = 100 - 100 * left;
      hint.textContent = left > 0 ? undoText : hintText;
      count.textContent = number.format(presses);
    };
    tile.addEventListener("click", () => {
      if (previous && Date.now() < undoUntil) {
        [start, presses] = previous;
        previous = null;
        undoUntil = 0;
      } else {
        previous = [start, presses];
        start = Date.now();
        presses++;
        undoUntil = start + 10000;
      }
      draw();
    });
    draw();
    setInterval(draw, 100);
  }

  // The chips follow the section in view.
  const chips = [...document.querySelectorAll(".chips a[href^='#']")];
  const sections = chips.map(a => document.querySelector(a.getAttribute("href")));
  const seen = new Map();
  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => seen.set(entry.target, entry.isIntersecting));
    const current = sections.find(section => seen.get(section));
    chips.forEach((a, i) => a.setAttribute("aria-current", String(sections[i] === current)));
  }, { rootMargin: "-40% 0px -55% 0px" });
  sections.forEach(section => observer.observe(section));
})();
"""


CURRENT = ' aria-current="page"'


def render(locale: str, strings: dict, captions: dict) -> str:
    s = strings[locale]
    folder, lang, _ = WEB[locale]
    up = "" if locale == DEFAULT else "../"  # From this page to docs/.
    rtl = locale == "ar"
    family, query = font(locale)
    script_font = f'\n<style>:root{{--script-font:"{family}"}}</style>' if family != "Noto Sans" else ""
    fonts = "family=Noto+Sans:wght@500;600;700;800" + (f"&family={query}:wght@500;600;700;800" if family != "Noto Sans" else "")
    tagline = captions[locale]["tagline"]
    shot_captions = captions[locale]["captions"]
    undo_hint = app_strings(locale)["tile_undo_hint"]
    og_locale = demo.ANDROID_LOCALE[locale].replace("in-", "id-").replace("-", "_")

    alternates = "\n".join(
        f'<link rel="alternate" hreflang="{WEB[other][1]}" href="{page_url(other)}">' for other in demo.LOCALES
    ) + f'\n<link rel="alternate" hreflang="x-default" href="{SITE}">'

    languages = "\n".join(
        f'      <li><a href="{up}{"" if other == DEFAULT else WEB[other][0] + "/"}" hreflang="{WEB[other][1]}" '
        f'lang="{WEB[other][1]}"{CURRENT if other == locale else ""}>'
        f'<span>{e(strings[other]["language"])}</span>{icon("check") if other == locale else ""}</a></li>'
        for other in demo.LOCALES
    )

    features = "\n".join(
        f'      <div class="tile {colour}{" wide" if wide else ""}"><span class="chip-icon">{icon(name)}</span>'
        f'<h3>{e(f["title"])}</h3><p>{e(f["text"])}</p></div>'
        for (colour, name, wide), f in zip(FEATURES, s["features"], strict=True)
    )

    shots = "\n".join(
        f'      <img class="raised" src="{up}img/{folder}/shot-{n}.webp" width="540" height="960" loading="lazy" '
        f'alt="{e(shot_captions[SHOT_CAPTIONS[n - 1]])}">'
        for n in range(1, SHOTS + 1)
    )

    return f"""<!doctype html>
<html lang="{lang}"{' dir="rtl"' if rtl else ""}>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{e(s["title"])}</title>
<meta name="description" content="{e(s["description"])}">
<meta name="theme-color" content="#ECEEF3" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#1B1D22" media="(prefers-color-scheme: dark)">
<link rel="canonical" href="{page_url(locale)}">
{alternates}
<link rel="icon" href="{up}img/icon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="{up}img/apple-touch-icon.png">
<meta property="og:type" content="website">
<meta property="og:site_name" content="Time Clicker">
<meta property="og:title" content="{e(s["title"])}">
<meta property="og:description" content="{e(s["description"])}">
<meta property="og:url" content="{page_url(locale)}">
<meta property="og:image" content="{SITE}img/{folder}/og.jpg">
<meta property="og:image:width" content="1024">
<meta property="og:image:height" content="500">
<meta property="og:locale" content="{og_locale}">
<meta name="twitter:card" content="summary_large_image">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?{fonts}&amp;display=swap">{script_font}
<link rel="stylesheet" href="{up}style.css">
<script>document.documentElement.classList.add("js")</script>
</head>
<body>
<main>
  <header class="appbar">
    <a class="logo" href="./"><img src="{up}img/icon.svg" alt="" width="48" height="48"><span>Time Clicker</span></a>
    <details class="lang">
      <summary class="neu-btn raised-sm" aria-label="{e(s["language_menu"])}">{icon("globe")}<span>{e(lang.split("-")[0].upper())}</span></summary>
      <ul class="raised">
{languages}
      </ul>
    </details>
  </header>

  <nav class="chips" aria-label="Time Clicker">
    <a class="raised-sm" href="#features">{e(s["nav_features"])}</a>
    <a class="raised-sm" href="#screenshots">{e(s["nav_screenshots"])}</a>
    <a class="raised-sm" href="#download">{e(s["nav_download"])}</a>
    <a class="raised-sm" href="{up}privacy.html">{e(s["nav_privacy"])}</a>
  </nav>

  <div class="hero">
    <div>
      <h1>{e(tagline)}</h1>
      <p class="intro">{e(s["intro"])}</p>
      <button class="tile sage demo" type="button" data-undo="{e(undo_hint)}">
        <span class="top"><span class="chip-icon">{icon("sprout")}</span><span class="ring"><svg class="arc" viewBox="0 0 36 36" aria-hidden="true"><circle cx="18" cy="18" r="17" pathLength="100"/></svg>{icon("undo-2")}</span></span>
        <span class="name">{e(s["demo_name"])}</span>
        <span class="when"></span>
        <span class="bottom"><span class="hint">{e(s["demo_hint"])}</span><span class="count">1</span></span>
      </button>
      <div class="get">{badge(locale, s)}</div>
    </div>
    <picture class="phone"><source srcset="{up}img/{folder}/hero-dark.webp" media="(prefers-color-scheme: dark)"><img src="{up}img/{folder}/hero.webp" width="760" height="1312" alt="{e(s["hero_alt"])}"></picture>
  </div>

  <section id="features">
    <h2>{e(s["features_title"])}</h2>
    <div class="features">
{features}
    </div>
  </section>

  <section id="screenshots">
    <h2>{e(s["screenshots_title"])}</h2>
    <div class="shots" tabindex="0" role="region" aria-label="{e(s["screenshots_title"])}">
{shots}
    </div>
  </section>

  <section id="download" class="card raised">
    <h2>{e(s["download_title"])}</h2>
    <p>{e(s["download_text"])}</p>
    <div class="get">{badge(locale, s)}<span class="apk">{linked(s["apk"], [APK])}</span></div>
  </section>

  <footer>
    <p>{linked(s["made_by"], [AUTHOR, LUCIDE])}</p>
    <p>{linked(s["photo_credit"], [PHOTO, PHOTO_LICENCE])}</p>
    <p><a href="{up}privacy.html" hreflang="en">{e(s["privacy"])}</a></p>
    <p>{e(s["trademark"])}</p>
  </footer>
</main>
<script>{SCRIPT}</script>
</body>
</html>
"""


def outputs() -> dict[Path, str]:
    strings = json.loads((HERE / "strings.json").read_text(encoding="utf-8"))
    captions = json.loads((ROOT / "tools/store/captions.json").read_text(encoding="utf-8"))
    missing = [locale for locale in demo.LOCALES if locale not in strings]
    if missing:
        raise SystemExit(f"strings.json has no text for {', '.join(missing)}")
    out = {}
    for locale in demo.LOCALES:
        path = DOCS / "index.html" if locale == DEFAULT else DOCS / WEB[locale][0] / "index.html"
        out[path] = render(locale, strings, captions)
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail if the pages in docs/ are out of date")
    args = parser.parse_args()
    stale = []
    for path, text in outputs().items():
        # Git may check the pages out with CRLF line endings.
        current = path.read_text(encoding="utf-8").replace("\r\n", "\n") if path.exists() else None
        if current == text:
            continue
        if args.check:
            stale.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8", newline="\n")
            print(path.relative_to(ROOT))
    if stale:
        print("Out of date (run python tools/web/build.py):\n  " + "\n  ".join(stale))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
