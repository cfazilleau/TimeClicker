# Website

The website, <https://timeclicker.cfaz.dev> (linked from the app's About screen), is the static `web/` folder:
`index.html`, `privacy.html` (the privacy policy linked from the Play listing: keep its URL) and `style.css`.
No build step.

`.github/workflows/pages.yml` publishes `web/` to GitHub Pages on every push to `main` that changes it, or from
**Actions → Website → Run workflow**. The repository's Pages source is **GitHub Actions** (**Settings → Pages**), and the
custom domain is set there too; `web/CNAME` records it.

To preview it locally, open `web/index.html` in a browser, or serve the folder:

```bash
python -m http.server -d web
```
