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
- [Updates](#updates)
- [First launch](#first-launch)
- [Features](#features)
- [Downloads and offline listening](#downloads-and-offline-listening)
- [Lyrics](#lyrics)
- [Play on another device or speaker](#play-on-another-device-or-speaker)
- [SpotiOS Server Mode](#spotios-server-mode)
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

## Updates

SpotiOS checks [GitHub Releases](https://github.com/hacker123as/SpotiOS/releases) for a new version when it opens
(at most every 6 hours). You can also check any time in **Settings → About → Check for updates**.

- When there's an update, a card shows what's new with **Update** and **Later**. Later hides that version for a day.
- **Update** downloads the new version and installs it over the one you have, so your login, settings and downloads
  stay. The first time, Android asks you to allow SpotiOS to install apps: tap **Allow**, turn it on and come back.
- SpotiOS closes for a moment while Android updates it. With Server mode on, the server starts again by itself.

Android only installs an update over the app when both are signed with the same key. If your SpotiOS was signed with
another key (an APK someone built and sent you, or an older build), SpotiOS saves the new version to **Downloads** and
asks for a one-time reinstall: tap **Uninstall**, then open the saved file. You'll need to sign in to Spotify again.
After that, updates install with one tap. Get SpotiOS from Releases so updates keep installing in place.

## First launch

The first launch walks you through four short screens:

1. **Welcome**: a quick look at what SpotiOS does.
2. **Normal or Server mode**: **Normal** is the full app. **Server mode** turns SpotiOS into a speaker for your
   normal Spotify app, see [SpotiOS Server Mode](#spotios-server-mode). You can switch later in Settings.
3. **Permissions**: notifications (for lock screen and notification controls) and nearby devices
   (for Bluetooth and Spotify Connect). You can skip this and allow them later in Android settings.
4. **Keep playing in the background**: tap **Turn off battery optimization**, then **Allow**.
   Without this, Android can pause music and downloads when the screen is off.
   If your phone shows a list instead of a dialog, find SpotiOS and set it to **Unrestricted** or **Not optimized**.

After you log in, SpotiOS skips Spotify's "you're logged in" page and opens Home.

If you set up an older version, you'll see the Normal or Server choice once on your next launch.
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
- **Queue** opens right inside Now Playing. Tap any song in it to skip straight to it.
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

The offline library looks like Spotify's: your downloaded playlists, albums and Liked Songs with their covers,
a page for each with its full track list, and songs you haven't downloaded shown grayed out.
Songs play inside their playlist, so next and previous stay in that playlist.

What works offline: songs you've downloaded play from the offline library, with their artwork and details.
What doesn't: Spotify's own pages (Home, Search, artist pages and streaming) need internet.
When your phone is offline, an "Offline mode" label shows under the tab bar, and if you open SpotiOS with no
connection it goes straight to your offline library.

## Lyrics

When Spotify has no lyrics for a song, SpotiOS looks them up on [LRCLIB](https://lrclib.net), a free and open
lyrics database. Synced lyrics scroll along with the song, and you can tap a line to jump to it.
Turn this on or off with **Settings → Lyrics for Every Song**.

## Play on another device or speaker

- In Now Playing, tap **Play on**. It opens inside the player and lists:
  - what's playing now, with a volume slider when it's another device,
  - **This phone**, and **Phone speaker or Bluetooth** for Android's output picker,
  - your other Spotify devices, such as a laptop, speaker or TV. Tap one to move the music there.
- It works both ways. Your phone shows up as **SpotiOS** in Spotify on your laptop or another phone, so you can send
  music to it from there. Change the name in **Settings → Player → Device name**.

## SpotiOS Server Mode

Server mode makes SpotiOS a Spotify Connect speaker that is always ready. Open your normal Spotify app (on this phone,
another phone, a laptop or anything with Spotify), tap the devices button, pick **SpotiOS**, and the music plays through
SpotiOS.

- **Turn it on** on the welcome screen or in **Settings → Playback → SpotiOS Server**.
- **It's ready right away.** You don't need to play anything on SpotiOS first: as soon as it's signed in, pick it in
  Spotify and the music starts there.
- **It keeps running** when you swipe SpotiOS away or lock the phone. A small notification shows that the server is on
  and whether it's connected; it stays in the notification shade even when nothing has played for a long time, with
  **Resume** for the last song. If Android ever stops SpotiOS, it starts again within 15 minutes (with battery
  optimization off).
- **Stop server**: the power button at the top of the Server screen (also in **⋯** and in the notification) leaves
  Spotify Connect and closes SpotiOS. Nothing starts it again, not even Start with Spotify, until you open SpotiOS.
- **It stays connected.** SpotiOS checks its link to Spotify every 20 seconds, checks that Spotify still lists it, and
  reconnects on its own after a network change or a dropped connection, so it doesn't vanish from the devices list.
- **The Server screen** replaces the full web player after you log in. It shows what's playing, with play, pause and skip,
  and your recents, playlists and liked songs, with search. Tap one to play it on SpotiOS. It uses the same Spotify
  service the web player itself uses. If Spotify is busy anyway, lists and play buttons try again by themselves, and
  your lists are kept, so they show at once next time.
- **Getting around**: the **⋯** button at the top right has **Browse Spotify** (the whole web player), **Play on another
  device**, **SpotiOS settings**, **Switch to Normal mode** and **Stop server**. While you browse, the **Server** tab at the left of the
  tab bar (or the back button) takes you back to the Server screen.
- **How to connect** is on the Server screen, with a button that opens the Spotify app.
- **Take over**: when Spotify starts playing on another device (open Spotify on your phone and press play), SpotiOS
  moves the music to itself within a second or two. It works from **any device**, or only **one device** you pick. To
  play somewhere else, pick the device under **Play on** on the Server screen: SpotiOS leaves the music there until
  that device stops. A device that takes the music straight back twice is left alone the same way, and devices that
  aren't allowed to play on SpotiOS are never taken over from.
- **Who can play**: allow every device, or only the ones you pick. A device that isn't allowed gets its music sent back.
- **Start with Spotify**: SpotiOS starts the server when the Spotify app plays on this phone. This needs notification
  access (SpotiOS only looks at Spotify's notifications) and battery optimization turned off for SpotiOS. Without that,
  Android doesn't let SpotiOS start in the background, and you get a "Tap to start" notification instead.

Server mode keeps the phone lightly awake so Spotify can always reach it, which uses some battery. Some phones
(Xiaomi, Huawei, OnePlus and others) close apps that are swiped away no matter what; on those, lock SpotiOS in the recent
apps screen or allow it to "auto start" in the phone's settings.

## Open Spotify links in SpotiOS

Song, album, playlist and artist links, `spotify:` links, and Spotify links you share to SpotiOS open in the app.
On Android 12 and newer, go to **Settings → Advanced → Open Spotify Links**, turn on **Open supported links** and add the
Spotify links. If the official Spotify app is installed, Android may still send links there.

## Settings guide

Open the **Settings** tab in the tab bar.

| Setting | What it does |
| --- | --- |
| Server mode | Keeps SpotiOS running in the background as a Spotify Connect speaker, see [SpotiOS Server Mode](#spotios-server-mode). |
| Start with Spotify | Starts the server when the Spotify app plays on this phone. Needs notification access. |
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
| Device name | The name your other devices see in Spotify Connect (default SpotiOS). |
| Developer mode | Turns on user scripts, the script installer and the Dev menu. |
| User scripts (Dev) | Add your own scripts, see below. |

Some things can't be done because Spotify's web player doesn't allow them: crossfade, audio quality, an equalizer and volume boost. Spotify's audio is copy-protected, so the app can't process the sound itself.

## Dev menu: user scripts

SpotiOS can run your own JavaScript on Spotify's pages, like Tampermonkey. It's off until you turn it on.

1. Go to **Settings → Advanced → Dev** and turn on **Developer mode**.
2. Install a script:
   - On [Greasy Fork](https://greasyfork.org/en/scripts/by-site/spotify.com), tap **Install** and pick SpotiOS.
     If your browser just shows the code, tap **Share** and pick SpotiOS instead.
   - Or use **Settings → Dev → Install from link**, or open a downloaded `.user.js` file.
   - SpotiOS shows the script's name, version, what it runs on and its code before you install it.
     Installing a script with the same name again updates it.
3. Manage scripts in **Settings → Dev → User scripts** or the **Dev** item in the account menu.
   New scripts start right away; turning one off takes effect when the page reloads.

Supported in scripts:
- The usual `// ==UserScript==` header, with `@name`, `@match`, `@include` and `@require` (fetched when you install).
- `GM_addStyle`, `GM_getValue`, `GM_setValue`, `GM_deleteValue`, `GM_listValues`, `GM_xmlhttpRequest`,
  `GM_setClipboard`, `GM_openInTab` and `GM_notification`, plus the `GM.*` promise versions.

Each script runs in its own `try/catch`, so a broken script can't break the app.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| Music or downloads stop when the screen is off | Turn off battery optimization in **Settings → Downloads → Background downloads**. |
| Spotify shows "Something went wrong" | SpotiOS recovers on its own. If it keeps happening, restart the app. |
| No sound | Open **Play on** in Now Playing and pick **This phone**. Another device may have taken over playback. |
| SpotiOS isn't in Spotify's devices list | Open SpotiOS once so it can sign in, and check the Server notification says it's ready. If it still isn't listed, tap **Reconnect now** on the Server screen. On the same Wi-Fi isn't needed: any device signed in to the same Spotify account sees it. |
| Server mode stops after a while | Turn off battery optimization for SpotiOS, and on phones that close swiped-away apps, lock SpotiOS in recent apps. |
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
`.github/workflows/keep-signing-key.yml` restores that cache twice a week so GitHub never evicts it, and fails loudly
if it's gone. Each release lists its signing certificate's SHA-256, so a key change is easy to spot.

### How it works

SpotiOS loads `open.spotify.com` in a WebView and adds its own CSS and JavaScript on top:

- `webview/injections/SpotiOSUi.kt`: the glass shell, including the tab bar, Now Playing, lyrics, Play on and the phone layouts.
- `webview/injections/AdCleaner.kt`: hides ads and mutes and skips audio ads.
- `webview/injections/ServerScreen.kt` and `ConnectKeepAlive.kt`: the Server screen, device rules and the Spotify Connect keepalive.
- `webview/PlayerHost.kt` and `service/ServerMode.kt`: keep the player running without the app screen in Server mode.
- `webview/helpers/UserScripts.kt`: the Dev menu's user scripts.
- `lyrics/LrcLib.kt`: lyrics lookup.
- `offline/`: downloads and the offline library.
- `update/` and `util/UpdateChecker.kt`: update checks against GitHub Releases and installing updates in place.
- `ui/onboarding/OnboardingFlow.kt`: the first-launch screens.

---

## Credits

SpotiOS is a fork of [Spotilol](https://github.com/lyssadev/Spotilol).
**deviato** reverse-engineered the original Spotifuck. **lyssadev** ported the core logic from smali to Kotlin.
Lyrics come from [LRCLIB](https://lrclib.net).

SpotiOS isn't affiliated with or endorsed by Spotify. You need your own Spotify account.

all rights reserved — lyssadev & deviato.
