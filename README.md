# Time Clicker

A small Android app that tracks *when you last did something*.
Each tile ("Watered the plants") shows how long ago it was last done, ticking live.
Tap a tile when you do it again and it resets to now; long-press it to edit it.
Groups, reminders, home-screen widgets, photos and about 1,900 icons, in 16 languages. Fully offline.

[Get it on Google Play](https://play.google.com/store/apps/details?id=dev.cfaz.timeclicker) ·
[download the APK](https://github.com/cfazilleau/TimeClicker/releases/latest) ·
[website](https://timeclicker.cfaz.dev)

Kotlin, Jetpack Compose, Room and Glance widgets; min SDK 26.

## Documentation

The full documentation is on the [wiki](https://github.com/cfazilleau/TimeClicker/wiki), copied from
[`wiki/`](wiki/Home.md) on every push to `main`:

- [Features](wiki/Features.md): everything the app does
- [Building](wiki/Building.md): build and test locally
- [Architecture](wiki/Architecture.md): tech stack, project layout, data model, database migrations
- [Icons](wiki/Icons.md): the bundled Lucide icons and their search words
- [Releasing](wiki/Releasing.md): CI, publishing to Google Play, [release signing](wiki/Releasing.md#release-signing)
- [Store listing](tools/store/README.md): the Play Store text and screenshots
- [Website](wiki/Website.md): the site in `docs/` and its privacy policy

## Quick start

Requires JDK 17+ and the Android SDK (Android Studio sets this up).

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
```
