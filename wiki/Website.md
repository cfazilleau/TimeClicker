# Website

The website, <https://timeclicker.cfaz.dev> (linked from the app's About screen), is the static `docs/` folder:
`index.html`, `privacy.html` (the privacy policy linked from the Play listing: keep its URL) and `style.css`.
No build step.

GitHub Pages serves it straight from `main`, folder `/docs` (**Settings → Pages**), on every push.
`docs/CNAME` holds the custom domain: without it, Pages drops the domain. `docs/.nojekyll` turns Jekyll off.
Keep anything that isn't part of the site out of `docs/`: these pages live in `wiki/`.

To preview it locally, open `docs/index.html` in a browser, or serve the folder:

```bash
python -m http.server -d docs
```
