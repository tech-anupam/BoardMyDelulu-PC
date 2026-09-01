<div align="center">
  <img src="logo.png" alt="BoardMyDelulu Logo" width="160"/>
  <h1>BoardMyDelulu PC</h1>
  <p><b>The only meme soundboard software you need on desktop. Instant viral Indian and global meme audio pads with zero bullshit and zero ads.</b></p>
  <p>
    <a href="https://github.com/tech-anupam/BoardMyDelulu-PC/releases/download/v1.0.0/BoardMyDelulu-1.0.0.msi"><img src="https://img.shields.io/badge/Download_Installer-.MSI-6C5CE7?style=for-the-badge&logo=windows" alt="Download MSI"/></a>
    <a href="https://github.com/tech-anupam/BoardMyDelulu-PC/releases/download/v1.0.0/BoardMyDelulu-1.0.0-Portable.zip"><img src="https://img.shields.io/badge/Download_Portable-.ZIP-FFA502?style=for-the-badge" alt="Download Portable ZIP"/></a>
    <a href="https://github.com/tech-anupam/BoardMyDelulu"><img src="https://img.shields.io/badge/Android_App-Available-00CEC9?style=for-the-badge&logo=android" alt="Android App"/></a>
    <a href="https://anupambuilds.store/donate"><img src="https://img.shields.io/badge/Donate-Support%20Creator-FF6B9D?style=for-the-badge" alt="Donate"/></a>
    <img src="https://img.shields.io/badge/License-MIT-2ECC71?style=for-the-badge" alt="License"/>
  </p>
</div>

<p align="center">
  <img src="preview/1_sounds.jpg" width="48%" alt="Sounds Feed" />
  <img src="preview/2_search.jpg" width="48%" alt="Search & Categories" />
</p>
<p align="center">
  <img src="preview/3_favorites.jpg" width="31%" alt="Favorites & Hotkeys" />
  <img src="preview/4_downloads.jpg" width="31%" alt="Downloads Manager" />
  <img src="preview/5_settings.jpg" width="31%" alt="Settings & Audio Routing" />
</p>

---

## Downloads

Download the latest version from the [v1.0.0 Release](https://github.com/tech-anupam/BoardMyDelulu-PC/releases/tag/v1.0.0):

| Package Type | Download Link | Description |
| :--- | :--- | :--- |
| **Windows Installer** | [BoardMyDelulu-1.0.0.msi](https://github.com/tech-anupam/BoardMyDelulu-PC/releases/download/v1.0.0/BoardMyDelulu-1.0.0.msi) | Standard installer with desktop shortcut, start menu entry, and custom install path chooser |
| **Portable Version** | [BoardMyDelulu-1.0.0-Portable.zip](https://github.com/tech-anupam/BoardMyDelulu-PC/releases/download/v1.0.0/BoardMyDelulu-1.0.0-Portable.zip) | Standalone portable executable. Extract and run `BoardMyDelulu.exe` directly without installing |

---

## Why BoardMyDelulu on PC?

Most desktop soundboard applications are bloated, riddled with trial limitations, subscription paywalls, or webview memory hogs. BoardMyDelulu is built natively on Compose Multiplatform with Skia rendering for maximum performance, minimum memory usage, and zero nonsense.

- **Global Hotkeys Anywhere**: Bind custom physical keys to any favorite or downloaded sound. Triggers instantly inside fullscreen games, Discord, OBS, and desktop apps.
- **Meme Deck (Shuffle & Cycle Mode)**: Press your dedicated Deck hotkey (default F8/F9) to randomly shuffle or sequentially cycle through your entire favorite sound collection while gaming.
- **Continuous Auto-Loop**: Option to auto-advance to the next sound in your meme playlist as soon as the current one finishes.
- **Panic Stop Key**: Press Escape or click Stop anytime to immediately kill all playing audio across the board.
- **Hardware & Software Output Routing**: Select your preferred physical or virtual audio output device (Speakers, Headphones, Virtual Cables, USB DACs) with live master volume and instant mute controls.
- **Instant Trending & Global Feeds**: Stream thousands of meme sound pads across India, United States, UK, Brazil, Japan, and worldwide.
- **Two-Tier Cache Engine**: Feeds and search results load with zero latency using RAM and local disk caching.
- **Offline Downloads**: Save sound files locally to your machine with one-click direct access to your local sounds folder.
- **Windows Startup Integration**: Toggle automatic launch on Windows boot directly from Settings.
- **Zero Ads, Zero Trackers**: Pure utility for gamers, streamers, creators, and meme connoisseurs.

---

## Building from Source

### Prerequisites
- Windows 10 or Windows 11 (64-bit)
- JDK 17 or newer

### Run in Development
```bash
gradle run
```

### Build Standalone Executable (Portable)
```bash
gradle createDistributable
```
The portable folder containing `BoardMyDelulu.exe` with bundled runtime will be created at:
```
build/compose/binaries/main/app/
```

### Build Windows MSI Installer
```bash
gradle packageMsi
```
The `.msi` installer wizard with custom install location chooser will be generated at:
```
build/compose/binaries/main/msi/
```

---

## Tech Stack

- **UI Framework**: Compose Multiplatform Desktop (Skia GPU Engine)
- **Language**: Kotlin 2.1.20
- **Audio Engine**: Custom Pure-PCM Streaming Engine with JavaZoom JLayer decoder
- **Global Input**: JNativeHook 2.2.2 system-wide low-level keyboard listener
- **Networking**: OkHttp 4.12.0 + Kotlinx Serialization
- **Scraper Fallback**: Jsoup 1.18.3 HTML Engine

---

## Companion Mobile App

Looking for the Android version?
Check out the official Android app repository: [BoardMyDelulu for Android](https://github.com/tech-anupam/BoardMyDelulu).

---

## Support Indie Development

If BoardMyDelulu saved your Discord calls or made your gaming lobbies hilarious, consider supporting development:

- **Donate Online**: [anupambuilds.store/donate](https://anupambuilds.store/donate)
- **UPI ID**: `anupambuilds@fam`
- **Developer**: Anupam Jha
- **Portfolio**: [anupambuilds.store/about](https://anupambuilds.store/about)
- **GitHub**: [@tech-anupam](https://github.com/tech-anupam)
- **Instagram**: [@tech.anupam](https://instagram.com/tech.anupam)
- **Email**: `killerboy99126@gmail.com`

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.
