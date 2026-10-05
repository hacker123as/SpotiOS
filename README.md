<p align="center">
  <img src="art/spotios-icon.png" width="112" alt="SpotiOS icon"/>
</p>

<h1 align="center">SpotiOS</h1>

<p align="center">
  A Spotify client for Android with an iOS 26 style liquid glass look.<br/>
  It runs Spotify's own web player inside the app and reshapes it to look and feel like a phone app.
</p>

<p align="center">
  <a href="https://github.com/hacker123as/SpotiOS/releases">
    <img src="https://img.shields.io/github/v/release/hacker123as/SpotiOS?include_prereleases&style=for-the-badge&logo=github&labelColor=0d0d0d&color=1DB954" alt="Download APK"/>
  </a>
</p>

---

## Contents

- [Install](#install)
- [First launch](#first-launch)
- [Features](#features)
- [Downloads and offline listening](#downloads-and-offline-listening)
- [Lyrics](#lyrics)
- [Play on another device or speaker](#play-on-another-device-or-speaker)
- [Settings guide](#settings-guide)
- [Dev menu: user scripts](#dev-menu-user-scripts)
- [Troubleshooting](#troubleshooting)
- [Build it yourself](#build-it-yourself)
- [Credits](#credits)

## Install

1. Download the newest `.apk` from [Releases](https://github.com/hacker123as/SpotiOS/releases).
2. Open it. Android may ask you to allow "Install unknown apps" for your browser or file manager first.
3. Open SpotiOS and log in with your Spotify account.

SpotiOS uses its own app id (`com.spotios.app`), so it installs next to Spotilol instead of replacing it.
It needs Android 9 or newer.

## First launch

The first launch walks you through three short screens:

1. **Welcome**: a quick look at what SpotiOS does.
2. **Permissions**: notifications (for lock screen and notification controls) and nearby devices
   (for Bluetooth and Spotify Connect). You can skip this and allow them later in Android settings.
3. **Keep playing in the background**: tap **Turn off battery optimization**, then **Allow**.
   Without this, Android can pause music and downloads when the screen is off.
   If your phone shows a list instead of a dialog, find SpotiOS and set it to **Unrestricted** or **Not optimized**.

After you log in, SpotiOS skips Spotify's "you're logged in" page and opens Home.

If you set up an older version, you'll see the battery screen once on your next launch.
You can always change it later in **Settings → Downloads → Background downloads**.

## Features

**Looks like the app**
- Liquid glass tab bar (Home, Search, Library, Settings), mini player and Now Playing sheet.
- Home has filter chips, a two-column grid of your shortcuts and shelves you swipe sideways.
- Artist pages open with a full-width photo, the artist's name in large type and a numbered Popular list.
- Playlist and album pages use a phone layout: centered cover, title, then the song list.
- Optional album-art wallpaper that tints the whole app with the colors of the current song.
- The mini player stays hidden until something is playing.
- Fits phones, foldables and tablets in portrait or landscape.

**Playing music**
- One tap on a song plays it right away.
- Full-screen Now Playing with scrubbing, shuffle, repeat, like, queue, lyrics, sleep timer and download.
  Tap or swipe up on the mini player to open it, swipe down to close.
- Media notification, lock screen controls, Bluetooth and headset buttons, and car controls.
- Music pauses when Bluetooth disconnects (on by default, can be turned off).
- Long-press songs, cards and library rows for quick actions such as add to queue, go to artist and share.
- The **⋯** button in Now Playing opens a menu: go to artist, go to album, open queue, share, your stats and **playback speed** (0.5× to 2×).
- **Gestures:** swipe the mini player or the artwork sideways to skip, double-tap the artwork to like, and optionally shake your phone to skip.
- **Listening stats:** minutes today, this week and all time, a 7-day chart, and your top songs and artists.
  Stats are kept on your phone only. Open them from Now Playing → ⋯ or the account menu.
- **Hide podcasts and audiobooks** to keep Home and Library about music.

**Ad blocking**
- Built in. No certificate, VPN or proxy is needed.
- Ad banners and upgrade prompts are hidden, and audio ads are muted and skipped.

## Downloads and offline listening

- Tap the download button on a song, album or playlist to save it.
- Downloads keep running in the background, even with the screen off, as long as battery optimization is off for SpotiOS.
- **Songs you already have are never downloaded twice.** If you download a playlist again, SpotiOS skips
  every song that's already saved and only fetches the ones that are missing. The summary at the end says
  how many were saved and how many you already had.
- Manage what's saved in **Settings → Downloads → Manage downloads**. Songs are grouped by playlist or album,
  and you can remove a whole group or everything at once.
- **Settings → Downloads → Open offline library** shows what you can play without internet.

What works offline: songs you've downloaded play from the offline library, with their artwork and details.
What doesn't: Spotify's own pages (Home, Search, artist pages and streaming) need internet.
When your phone is offline, an "Offline mode" label shows under the tab bar, and if you open SpotiOS with no
connection it goes straight to your offline library.

## Lyrics

When Spotify has no lyrics for a song, SpotiOS looks them up on [LRCLIB](https://lrclib.net), a free and open
lyrics database. Synced lyrics scroll along with the song, and you can tap a line to jump to it.
Turn this on or off with **Settings → Lyrics for Every Song**.

## Play on another device or speaker

- In Now Playing, tap the devices button to open **Play on**.
  - **This phone** opens Android's output picker, where you can choose the phone speaker or a Bluetooth device.
  - **Spotify Connect** lists your other Spotify devices, such as a laptop, speaker or TV.
- It works both ways. SpotiOS shows up as a device in Spotify on your laptop or another phone, so you can send music to it from there.

## Settings guide

Open the **Settings** tab in the tab bar.

| Setting | What it does |
| --- | --- |
| iOS Tab Bar | Shows the floating Home, Search, Library and Settings bar. |
| Album Art Wallpaper | Tints the app with the current song's artwork. |
| Haptic Feedback | Small vibrations when you tap tabs and player buttons. |
| Lyrics for Every Song | Uses LRCLIB when Spotify has no lyrics. |
| Swipe to Skip | Swipe the mini player or artwork sideways to change songs. |
| Double-Tap to Like | Double-tap the artwork in Now Playing to save the song. |
| Shake to Skip | Shake the phone to skip while the screen is on (off by default). |
| Hide Podcasts & Audiobooks | Hides podcast and audiobook shelves and chips. |
| Listening Stats | Counts your plays on this phone. |
| Hide Empty Mini Player | Hides the mini player until a song plays. |
| Pause on Disconnect | Pauses when Bluetooth headphones or the car disconnect. |
| Play Here on Launch | Moves playback to this phone when SpotiOS opens. |
| Background downloads | Opens the battery setting so downloads keep going. |
| Manage downloads | Lists saved music and lets you remove it. |
| Open offline library | Shows what plays without internet. |
| User scripts (Dev) | Add your own scripts, see below. |

Some things can't be done because Spotify's web player doesn't allow them: crossfade, audio quality, an equalizer and volume boost. Spotify's audio is copy-protected, so the app can't process the sound itself.

## Dev menu: user scripts

SpotiOS can run your own JavaScript on Spotify's pages, like Tampermonkey.

1. Tap your account picture, then **Dev** (or go to **Settings → Dev → User scripts**).
2. Tap **Add**, then paste a script or install one from a URL.
3. Turn scripts on or off with the switch. SpotiOS reloads the page when you close the list.

Supported in scripts:
- The usual `// ==UserScript==` header, with `@name`, `@match` and `@include`.
- `GM_addStyle`, `GM_getValue`, `GM_setValue`, `GM_deleteValue`, `GM_listValues`, `GM_xmlhttpRequest`,
  `GM_setClipboard`, `GM_openInTab` and `GM_notification`, plus the `GM.*` promise versions.

Each script runs in its own `try/catch`, so a broken script can't break the app.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| Music or downloads stop when the screen is off | Turn off battery optimization in **Settings → Downloads → Background downloads**. |
| Spotify shows "Something went wrong" | SpotiOS recovers on its own. If it keeps happening, restart the app. |
| No sound | Open **Play on** in Now Playing and pick **This phone**. Another device may have taken over playback. |
| No lock screen controls | Allow notifications for SpotiOS in Android settings. |
| A song won't download | Try again later, or tap skip on the download progress pill to move on. |

## Build it yourself

```bash
git clone https://github.com/hacker123as/SpotiOS
cd SpotiOS
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. GitHub Actions also builds every push
(`.github/workflows/build.yml`) and publishes the APK as a release.

To sign releases with your own permanent key, add these repository secrets:
`SPOTIOS_KEYSTORE_B64` (base64 of the `.jks`), `SPOTIOS_STORE_PASSWORD`, `SPOTIOS_KEY_ALIAS` and
`SPOTIOS_KEY_PASSWORD`. Without them, CI generates a key once and reuses it from the Actions cache.

### How it works

SpotiOS loads `open.spotify.com` in a WebView and adds its own CSS and JavaScript on top:

- `webview/injections/SpotiOSUi.kt`: the glass shell, including the tab bar, Now Playing, lyrics, Play on and the phone layouts.
- `webview/injections/AdCleaner.kt`: hides ads and mutes and skips audio ads.
- `webview/helpers/UserScripts.kt`: the Dev menu's user scripts.
- `lyrics/LrcLib.kt`: lyrics lookup.
- `offline/`: downloads and the offline library.
- `ui/onboarding/OnboardingFlow.kt`: the first-launch screens.

---

## Credits

SpotiOS is a fork of [Spotilol](https://github.com/lyssadev/Spotilol).
**deviato** reverse-engineered the original Spotifuck. **lyssadev** ported the core logic from smali to Kotlin.
Lyrics come from [LRCLIB](https://lrclib.net).

SpotiOS isn't affiliated with or endorsed by Spotify. You need your own Spotify account.

all rights reserved — lyssadev & deviato.
