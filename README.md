# Kin 🤍

A private Android photo app for the people closest to you — one album per person.

## Features
- **One album per person** — name, relation (Family, Partner, Best friend…), auto cover or pick your own
- **Photos are copied into Kin's private storage** — they stay even if deleted from your gallery, and don't show up in other gallery apps
- **Share to Kin** from Gallery, WhatsApp, Instagram etc. → choose whose album it goes in
- Full-screen viewer with swipe, pinch-to-zoom, double-tap zoom
- Favorites, set cover, move between people, multi-select (long-press) to share/move/delete
- Search people, pin people to the top
- **App lock** (4-digit PIN, re-locks after 1 minute in background)
- **Private mode** — blocks screenshots and hides the app preview in recents
- **Export to gallery** (Pictures/Kin/<name>) for backups

## Get the APK
Every push to `main` builds automatically via GitHub Actions.

1. Open the repo's **Releases** (right side of the repo page) → latest **Kin build N** → download **Kin.apk**
2. Or: **Actions** tab → **Build APK** → latest run → artifact **Kin-apk**
3. To rebuild manually: **Actions → Build APK → Run workflow**

All builds are signed with the same key (`app/kin.keystore`), so a new build installs **over** the old one and keeps your photos.

> ⚠️ Uninstalling Kin deletes its photos. Use **album menu → Export to gallery** before uninstalling.

## Tech
Kotlin · Jetpack Compose · Material 3 · Coil · minSdk 26 (Android 8+) · no permissions required (uses the system photo picker).
