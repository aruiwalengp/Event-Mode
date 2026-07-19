# Event Shot v2 — Event Mode (Watcher)

Use your phone's NORMAL camera app with all its features (portrait, night,
zoom, video, pro mode). While an event is active, every new photo & video
is automatically renamed and moved into Pictures/EVENT_NAME/ within seconds.

## Upload to GitHub (one time)

1. Extract this ZIP on your PC -> open the `eventshot` folder
2. GitHub repo -> Add file -> Upload files -> DRAG all the CONTENTS in
   (app, .github, build.gradle.kts, settings.gradle.kts, gradle.properties, README.md)
3. Commit changes
4. Actions tab -> "Build APK" runs automatically (~3-4 min, green tick)
5. Open the run -> Artifacts -> download EventShot-APK
6. Copy app-debug.apk to phone -> tap -> Install (allow unknown apps)

NOTE: If `.github` folder is missing in the repo after upload, create it
manually: Add file -> Create new file -> name it `.github/workflows/build.yml`
and paste the content from that file.

## Use

1. Open Event Shot -> type event name -> START EVENT
2. First time: allow "All files access" + notifications
3. Camera opens automatically. Shoot photos & videos normally.
4. Everything lands in Pictures/EVENT_NAME/ named EVENT_NAME_date_time.jpg
5. Press STOP EVENT when done. Gallery/Google Photos shows the event album.
