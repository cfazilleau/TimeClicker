# Releasing

## CI: building and releasing on GitHub

`.github/workflows/build-apk.yml` runs on every push to `main`, on every pull request, and on manual dispatch:

- runs the unit tests and builds a minified release APK and app bundle
- uploads them as workflow artifacts (`TimeClicker-<version>.apk`, and `TimeClicker-<version>.aab` with its R8 mapping)
- on a version tag push, also creates a GitHub Release with the APK attached and generated release notes, and uploads
  the app bundle and the store listing to Google Play (see below)

`versionCode` is the workflow run number, so each new APK installs as an update over the previous one,
and each bundle is newer than the last one on Play. `versionName` is the tag without its `v`.

## Cutting a beta or a release

Tag the commit on `main` and push the tag:

```bash
git tag v1.8.0-beta.1 && git push origin v1.8.0-beta.1
```

A tag with a suffix (`v1.8.0-beta.1`, `v1.8.0-rc.1`: anything after a `-`) is a **beta**: a GitHub pre-release, and
Google Play's internal testing track (or the `PLAY_BETA_TRACK` repository variable's track: `alpha` is closed testing,
`beta` open testing). Fix and tag `-beta.2` as needed, then tag the same version without a suffix for the **release**:

```bash
git tag v1.8.0 && git push origin v1.8.0
```

It becomes the latest GitHub release and goes to Google Play's production track, rolled out to everyone once Google's
review passes. A release is built again from its own tag, as a new `versionCode`.

The release notes list the pull requests merged since the previous tag, by title (`tools/release/changelog.py`): for a
beta, since the previous tag of either kind; for a release, since the previous release, so they cover its betas. Label a
pull request `skip-changelog` to leave it out (CI or docs changes, say). Google Play gets the same list in plain text,
cut to its 500 characters, as the release notes of every store language. To write them by hand instead (translated),
edit `fastlane/metadata/android/*/changelogs/default.txt` before tagging: when one of them changed since the previous
tag, Google Play gets them as committed.

## Publishing to Google Play

The `google-play` job uploads the bundle with [fastlane supply](https://docs.fastlane.tools/actions/supply/), along with
the store listing as committed in `fastlane/metadata/android` (text, screenshots, feature graphic, release notes from
`changelogs/default.txt`, unless replaced by generated notes, above); it doesn't regenerate it. Images already on
Play are only sent again when they change. It runs on version tags, or from **Actions → Build and release → Run
workflow** with a track picked (with the committed release notes).
The listing itself, and how its screenshots are made: [Store listing](https://github.com/cfazilleau/TimeClicker/blob/main/tools/store/README.md).

Setup, once:

1. In the Play Console, create the app (`dev.cfaz.timeclicker`) and upload the **first** bundle by hand
   (a `TimeClicker-<version>-bundle` workflow artifact): Google's API can't make an app's first upload.
2. In Google Cloud, create a service account with a JSON key; in the Play Console (**Users and permissions**), invite its
   email with release permissions for this app.
3. Add the key's JSON as the `PLAY_SERVICE_ACCOUNT_JSON` repository secret. The release signing secrets below must be
   set too: Play only accepts bundles signed with your upload key.
4. In the Play Console, set up the beta testing track (**Testing → Internal testing**, or closed or open testing with
   `PLAY_BETA_TRACK`): add testers, and share the opt-in link with them.
5. Optional repository variables: `PLAY_BETA_TRACK` (`internal` by default; or `alpha`, `beta`) and
   `PLAY_RELEASE_STATUS` (`completed` by default, which releases straight away; `draft` leaves the release for you to
   roll out in the Console).

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
