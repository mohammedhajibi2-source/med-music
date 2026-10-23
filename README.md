# Med music — Android starter project

A dark red/black local music player prototype. It scans audio indexed by Android MediaStore and plays tracks using AndroidX Media3 ExoPlayer.

## Features
- Red/black goth-inspired home screen
- Scan and list music stored on the device
- Tap a track to play it
- Previous, play/pause, next controls
- No account or network connection required for local playback

## Build an APK (Android Studio)
1. Install Android Studio on a computer.
2. Open this project folder in Android Studio.
3. Allow Gradle sync to finish. Use JDK 17 if Android Studio asks.
4. Connect an Android phone with USB debugging enabled, or use an emulator.
5. To build an installable debug APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Android Studio will show a notification with **Locate**. The file is normally at `app/build/outputs/apk/debug/app-debug.apk`.
7. Transfer the APK to your phone, open it, and allow installation from that source if Android asks.

## Notes
- This is a starter implementation, not a production Spotify clone.
- It plays music files available through Android's media library; it does not stream Spotify, YouTube, or other online catalogs.
- To publish, create a signed release APK/AAB and review current Google Play policies.
- Package id: `com.medmusic.app`
