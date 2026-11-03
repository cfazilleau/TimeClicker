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

The full documentation is in [`docs/`](docs/Home.md):

- [Features](docs/Features.md): everything the app does
- [Building](docs/Building.md): build and test locally
- [Architecture](docs/Architecture.md): tech stack, project layout, data model, database migrations
- [Icons](docs/Icons.md): the bundled Lucide icons and their search words
- [Releasing](docs/Releasing.md): CI, publishing to Google Play, [release signing](docs/Releasing.md#release-signing)
- [Store listing](tools/store/README.md): the Play Store text and screenshots
- [Website](docs/Website.md): the `web/` folder and its privacy policy

## Quick start

Requires JDK 17+ and the Android SDK (Android Studio sets this up).

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
```
