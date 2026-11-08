# BandTracker

An Android app to keep a local record of **cash** income and expenses, organized into independent accounts, each with its own currency. It does not move money or reconcile anything: every account is just a searchable ledger.

## How it works

- Each **account** has a name and an ISO 4217 currency that cannot be changed after creation.
- Each **entry** has an amount, a date (never in the future), an optional subject and an optional note.
- Amounts are stored as integers in the currency's minor unit, never as floats. The decimals per currency are fixed by the app (COP and USD: 2, JPY: 0), so they are the same on every Android version.
- The history shows one account at a time, grouped by day. Amounts carry a `+` / `−` sign in addition to their color.
- **Export** and **import** use JSON and the system file picker. Importing merges by `uuid`: it only adds what does not exist, is all-or-nothing, and never overwrites or deletes data.
- Everything stays on your device: the app declares no permissions (not even network), has no Google Play services, no analytics, and does no background work. `allowBackup` is disabled, so the manual export is the only backup.
- Spanish and English, following the device language, with the system light/dark theme.

## Requirements

Android 13 (API 33) or newer. Tested on a Pixel 9a running GrapheneOS.

## Install

1. Download the APK from the [GitHub Releases](https://github.com/v4atu/BandTracker/releases) page (it can also be followed with [Obtainium](https://github.com/ImranR98/Obtainium)).
2. [Verify the signing certificate](#verify-the-signing-certificate).
3. Install the APK.

## Verify the signing certificate

Every official release is signed with the same key. Before installing, check that the APK's certificate matches this fingerprint:

| | |
| :- | :- |
| SHA-256 | `DB:7C:25:45:37:FF:02:35:B6:58:2D:C0:46:76:FA:12:A8:15:C3:11:4A:E3:0B:A5:50:15:42:0F:DB:85:4A:5A` |

**On a computer**, with `apksigner` from the Android SDK build-tools:

```sh
apksigner verify --print-certs BandTracker.apk
```

The `Signer #1 certificate SHA-256 digest` line must read (lowercase hex, no colons):

```
db7c254537ff0235b6582dc04676fa12a815c3114ae30ba55015420fdb854a5a
```

**On the phone**, you can use an app such as [AppVerifier](https://github.com/soupslurpr/AppVerifier): provide the package name `com.v4atu.bandtracker` and the SHA-256 above, then select the APK.

If the fingerprint does not match, **do not install the APK**. Android also refuses to update an installed app with a build signed by a different key. An APK you build yourself will have a different fingerprint.

## Permissions

None. The app requests no permissions. Export and import go through the system file picker, so no storage access is needed.

## Usage

Create an account (name and currency), then use **Income** or **Expense** on the home screen to add an entry. **History** lists the entries of the selected account; tap one to edit or delete it. **Accounts** lets you rename or delete accounts and export or import your data. The **?** button opens help, the app version and the third-party licenses.

## Build from source

```sh
./gradlew :app:assembleDebug                 # debug APK
./gradlew :app:testDebugUnitTest             # JVM unit tests
./gradlew :app:connectedDebugAndroidTest     # instrumented tests (needs a device)
./gradlew :app:assembleRelease               # release APK
scripts/verify-spec.sh [--with-unit-tests]   # checks the privacy, stack and i18n rules of the spec
```

The build has no signing configuration. Official releases are signed from Android Studio (Build → Generate Signed App Bundle / APK) with a keystore that is kept outside the repository. Without it, the release APK is unsigned.

## Project structure

Package `com.v4atu.bandtracker`:

- `domain/` – pure Kotlin logic (amounts, currency decimals, dates, text normalization), unit-tested on the JVM.
- `data/` – Room database (accounts, entries, DAOs, repositories) and the selected account.
- `backup/` – JSON model, codec and export/import service.
- `ui/` – Compose screens (home, entry, history, accounts, help), components, theme and navigation.

Behavior is specified in [docs/SPEC.md](docs/SPEC.md) and the design hand-off is in [docs/DESIGN.md](docs/DESIGN.md).

## License

[MIT](LICENSE). The Istok Web font (SIL OFL 1.1) and the Lucide icons (ISC) keep their own licenses, found in [docs/licenses](docs/licenses).
