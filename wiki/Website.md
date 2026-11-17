# Website

The website, <https://timeclicker.cfaz.dev> (linked from the app's About screen), is the static `docs/` folder.
GitHub Pages serves it straight from `main`, folder `/docs` (**Settings → Pages**), on every push: no build step to
deploy, the generated pages are committed.
`docs/CNAME` holds the custom domain: without it, Pages drops the domain. `docs/.nojekyll` turns Jekyll off.
Keep anything that isn't part of the site out of `docs/`: these pages live in `wiki/`.

## What's in it

| Path | |
|------|-|
| `index.html`, `<lang>/index.html` | The landing page, in the app's 16 languages (English at the root; `fr/`, `de/`, `pt-br/`, `zh-cn/`…). **Generated**: don't edit them by hand |
| `privacy.html` | The privacy policy, English only, written by hand. Linked from the Play listing: keep its URL |
| `style.css` | Shared styles: the app's palette and neumorphic tiles (mirrors `ui/theme/Theme.kt`, `Neumorphism.kt`, `home/TileCard.kt`) |
| `img/` | Icon, favicon, and per language the screenshots, the hero and the link-preview image. **Generated** |
| `favicon.ico` | Generated |

The look follows the app: Noto Sans (as on the store graphics), the home screen's title row and group chips, features
as tiles in the preset colours, light and dark from the system theme. The hero has a live tile that ticks and can be
tapped and undone, like a tile in the app.

## Changing the pages

- **Text**: [`tools/web/strings.json`](../tools/web/strings.json), one block per store locale. `{braces}` mark link
  text; the links themselves are in `build.py`, in order. The headline and the screenshot captions come from the store's
  [`tools/store/captions.json`](../tools/store/captions.json), the undo hint from the app's `tile_undo_hint`.
- **Layout and features** (colour and Lucide icon of each tile, which are wide): [`tools/web/build.py`](../tools/web/build.py).
- **Styles**: `docs/style.css`, by hand.

Then regenerate the pages (stdlib only):

```bash
python tools/web/build.py
```

`python tools/web/build.py --check` fails if the committed pages are out of date.

## Images

`docs/img/` is made from the store graphics in `fastlane/metadata/android/` ([store listing](../tools/store/README.md)):
the phone screenshots, the first one's screen (and the dark-theme one's) as the hero, the feature graphic as the
link-preview image, the launcher icon. After regenerating the store graphics (needs Pillow):

```bash
python tools/web/images.py
python tools/web/build.py
```

## Preview

Open `docs/index.html` in a browser, or serve the folder:

```bash
python -m http.server -d docs
```
