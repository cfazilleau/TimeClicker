# Building

Requires JDK 17+ and the Android SDK (Android Studio sets this up).

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests (database migrations), on a device or emulator
```

The debug build installs as a separate app (`dev.cfaz.timeclicker.debug`), so it can live next to the release build.

Release builds are made by CI: see [Releasing](Releasing.md).
