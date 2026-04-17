# URL Tracker Remover

Android utility that strips tracking parameters (utm_*, fbclid, gclid, igshid, …) from URLs. Share a link to it from any app and it returns a clean URL to the clipboard — optionally re-sharing through the system share sheet.

Rule data comes from the [ClearURLs](https://gitlab.com/ClearURLs/Rules) project (~200 providers, thousands of rules, covering Facebook, Instagram, Reddit, YouTube, Amazon, TikTok, X/Twitter, LinkedIn, Google, Bing, Spotify, AliExpress, and many more), plus `globalRules` that clean `utm_*`, `fbclid`, `gclid`, `mc_cid`, `_hsenc` etc. on any site.

## Features

- **Share target**: receive any shared text, extract the first URL, clean it.
- **Paste box on main screen**: read-only text field, long-press to paste — shows before/after and auto-copies the clean URL.
- **Configurable behavior**: after cleaning, choose to (a) put on clipboard, (b) re-trigger the system share sheet, or (c) both.
- **Optional history**: opt-in log of cleanings, pure text, no page previews. View screen supports copy-single, copy-all, and clear-all.
- **Manual rule updates**: fetch the latest ClearURLs `data.min.json` with one tap.
- **Referral-marketing toggle**: separately enable/disable removal of `ref`, `affiliate_id`, etc.

## Install

Grab the signed APK from:
- **GitHub Releases** (tagged builds): https://github.com/changyow-ai/remove-tracking-code-android/releases
- **GitHub Actions artifacts** (per-commit, 90-day retention).

## Build

Requires JDK 17+ and the Android SDK. From repo root:

```bash
./gradlew assembleDebug     # unsigned debug APK
./gradlew assembleRelease   # signed with the bundled keystore
./gradlew testDebugUnitTest # run UrlCleaner tests
```

## Signing

The repo ships a self-signed `app/signing/release.jks` with the alias `urlcleaner` and password `urlcleaner`. This is **public and not a proof of identity** — it exists so CI can produce an installable APK out of the box.

Android requires all updates to be signed by the same key that signed the previous install, so if you fork and want your users to be able to upgrade from your build, replace the keystore:

```bash
keytool -genkey -v -keystore app/signing/release.jks -alias urlcleaner \
  -keyalg RSA -keysize 2048 -validity 9125 \
  -storepass <your-pw> -keypass <your-pw> \
  -dname "CN=<you>, O=<you>, C=<cc>"
```

…and set these GitHub Action secrets (CI will prefer env vars over the committed defaults):

- `URLCLEANER_KEYSTORE_PASSWORD`
- `URLCLEANER_KEY_ALIAS`
- `URLCLEANER_KEY_PASSWORD`

## Rule updates

Open the app → Settings → **立即同步規則表 / Update rules now**. The app fetches `https://rules2.clearurls.xyz/data.min.json` and stores it in app-private storage; the bundled copy is used until then.

## Licenses

- Application code: **MIT** (see `LICENSE`).
- Bundled rules data (`app/src/main/assets/clearurls-rules.json`): **LGPL-3.0**, sourced from the ClearURLs project (see `NOTICE`).
