<h1 align="center">SpotiOS</h1>

<p align="center">
  Spotify's web player wrapped in an iOS 26 "Liquid Glass" Android app, with built-in adblocking.
  A fork of <a href="https://github.com/lyssadev/Spotilol">Spotilol</a>.
</p>

<p align="center">
  <a href="https://github.com/hacker123as/SpotiOS/releases">
    <img src="https://img.shields.io/github/v/release/hacker123as/SpotiOS?include_prereleases&style=for-the-badge&logo=github&labelColor=0d0d0d&color=1DB954" alt="Download APK"/>
  </a>
</p>

---

## Download

Grab the newest `.apk` from [Releases](https://github.com/hacker123as/SpotiOS/releases) and install it.
Android may ask you to allow "Install unknown apps" first. SpotiOS uses its own app id
(`com.spotios.app`), so it installs next to Spotilol instead of replacing it.

## What SpotiOS adds

- **Liquid Glass look**: frosted glass surfaces with light rims, a slow aurora wallpaper, rounded
  artwork and springy press animations across every Spotify page.
- **Docked mini player** that floats above everything and stays reachable while you browse.
- **Full-screen Now Playing**: large artwork that pulls its colors into the background, scrubbing,
  shuffle/repeat, like, volume, lyrics, queue, sleep timer, download and picture-in-picture.
  Tap or swipe up on the mini player to open it, swipe down to close.
- **Floating tab bar**: Home, Search, Library and Settings, iOS style. Toggle it in Settings → iOS Tab Bar.
- **Long-press quick actions** on songs, cards and library rows (add to queue, add to playlist, like, go to artist or album, share…).
- **Native share sheet** for the current song, and Spotify's "Copy link" also opens Android's share menu.
- **Sleep timer presets**: 5, 15, 30, 45 min, 1 hour, end of song, or a custom time.
- **Low power**: turning on Power Save also switches off blur and animations.
- Lock-screen, notification and Bluetooth/headset controls, widgets, Android Auto, offline
  downloads and adblocking from Spotilol all carry over.
- Phones, foldables and tablets in portrait or landscape.

## Build it yourself

```bash
git clone https://github.com/hacker123as/SpotiOS
cd SpotiOS
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Every push is also built by GitHub Actions
(`.github/workflows/build.yml`), which publishes the APK as a release.

To sign releases with your own permanent key, add these repository secrets:
`SPOTIOS_KEYSTORE_B64` (base64 of the `.jks`), `SPOTIOS_STORE_PASSWORD`, `SPOTIOS_KEY_ALIAS`,
`SPOTIOS_KEY_PASSWORD`. Without them, CI generates a key once and reuses it from the Actions cache.

---

## Credits

**deviato** reverse-engineered the original Spotifuck. **lyssadev** ported the core logic from smali to Kotlin and maintains this project.

all rights reserved — lyssadev & deviato.