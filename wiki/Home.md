# Time Clicker

A small Android app that tracks *when you last did something*. Each tile shows how long ago it was last done,
ticking live; tap it when you do it again.
[Get it on Google Play](https://play.google.com/store/apps/details?id=dev.cfaz.timeclicker),
or [download the APK](https://github.com/cfazilleau/TimeClicker/releases/latest).

## Using the app

- [Features](Features.md): tiles, groups, reminders, widgets, settings, languages

## Working on the app

- [Building](Building.md): build and test locally
- [Architecture](Architecture.md): tech stack, project layout, data model, automatic reminders, database migrations
- [Icons](Icons.md): the bundled Lucide icons and their search words
- [Releasing](Releasing.md): CI, Google Play, release signing
- [Store listing](https://github.com/cfazilleau/TimeClicker/blob/main/tools/store/README.md): the Play Store text and screenshots
- [Website](Website.md): timeclicker.cfaz.dev and its privacy policy

## About this documentation

These pages are the `wiki/` folder of the repository, copied to the
[wiki](https://github.com/cfazilleau/TimeClicker/wiki) on every push to `main` (`.github/workflows/wiki.yml`).
Edit them in `wiki/`, along with the code they describe: changes made on the wiki are overwritten.
Pages link to each other as `Page.md`, so the links work in both places.
