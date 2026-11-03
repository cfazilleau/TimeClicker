# Releasing

## CI: building and releasing on GitHub

`.github/workflows/build-apk.yml` runs on every push to `main`, on every pull request, and on manual dispatch:

- runs the unit tests and builds a minified release APK and app bundle
- uploads them as workflow artifacts (`TimeClicker-<version>.apk`, and `TimeClicker-<version>.aab` with its R8 mapping)
- on a tag push `vX.Y.Z`, also creates a GitHub Release with the APK attached, and uploads the app bundle and the
  store listing to Google Play (see below):

  ```bash
  git tag v1.0.0 && git push origin v1.0.0
  ```

`versionCode` is the workflow run number, so each new APK installs as an update over the previous one,
and each bundle is newer than the last one on Play.

## Publishing to Google Play

The `google-play` job uploads the bundle with [fastlane supply](https://docs.fastlane.tools/actions/supply/), along with
the store listing as committed in `fastlane/metadata/android` (text, screenshots, feature graphic, release notes from
`changelogs/default.txt`); it doesn't regenerate it. Images already on Play are only sent again when they change.
It runs on version tags, or from **Actions → Build and release → Run workflow** with a track picked.
The listing itself, and how its screenshots are made: [Store listing](https://github.com/cfazilleau/TimeClicker/blob/main/tools/store/README.md).

Setup, once:

1. In the Play Console, create the app (`dev.cfaz.timeclicker`) and upload the **first** bundle by hand
   (a `TimeClicker-<version>-bundle` workflow artifact): Google's API can't make an app's first upload.
2. In Google Cloud, create a service account with a JSON key; in the Play Console (**Users and permissions**), invite its
   email with release permissions for this app.
3. Add the key's JSON as the `PLAY_SERVICE_ACCOUNT_JSON` repository secret. The release signing secrets below must be
   set too: Play only accepts bundles signed with your upload key.
4. Optional repository variables: `PLAY_TRACK` (`internal` by default; or `alpha`, `beta`, `production`) and
   `PLAY_RELEASE_STATUS` (`draft` by default, which you then roll out in the Console; Play refuses anything else until
   the app's first release is published, then `completed` releases straight away).

The job uses the `google-play` environment: add required reviewers to it (**Settings → Environments**) to approve each
upload. With Play App Signing, installs from Play are signed by Google's key, so they don't update from (or to) the
GitHub APK: uninstall to switch.

## Release signing

Android only installs an update over an existing app if both are signed with the **same key**.
Without these secrets, CI signs with a throwaway key. Each build then needs an uninstall first,
which **erases your data**. Set up a permanent key, once:

```bash
keytool -genkeypair -v -keystore release.jks -alias timeclicker \
  -keyalg RSA -keysize 4096 -validity 10000
```

Then add these repository secrets (Settings → Secrets and variables → Actions, or with the `gh` CLI):

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` (on macOS: `base64 -i release.jks`) |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `timeclicker` (whatever alias you used) |
| `KEY_PASSWORD` | the key password (optional: defaults to the keystore password, which is what `keytool` uses by default) |

Back up `release.jks` and its passwords outside the repo. If you lose them, you can't ship updates to an existing install.
`*.jks` is git-ignored.
