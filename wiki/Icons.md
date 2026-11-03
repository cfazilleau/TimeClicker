# Icons

Tile icons are [Lucide](https://lucide.dev) icons, stored by name (`circle-check`). Earlier versions stored their
own names (`check`, `paw`…); database migration 3 → 4 and the import of format-1 backups rename them.

## Updating Lucide

`python tools/lucide/generate_icons.py` downloads the latest Lucide release and its categories and rewrites
`app/src/main/assets/lucide/icons.tsv`, each icon flattened to one path for both Compose and the widgets.
Before committing a regenerated file, check its diff for removed icons: a tile using one would lose its icon
(the unit tests check the default ones).

## Search words in other languages

Lucide's search tags are only in English. `assets/lucide/tags-<language>.tsv` adds search words in another language
(one icon per line: name, tab, comma-separated words), used when the app is in that language; `tags-fr.tsv` covers
every icon. They are written by hand, not generated: the script lists the icons a file is missing after an update,
and the unit tests fail on lines for icons Lucide no longer has. Another language only needs its own file.
