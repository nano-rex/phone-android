# Convoy Phone

Minimal standalone Android phone app scaffold.

Current scope:
- dial pad
- contacts list and search
- recent calls list
- recordings list and playback
- settings for dark mode, call recording, recording source, and recordings folder

Implementation notes:
- plain Java Android app
- no external runtime dependencies in `app/build.gradle`
- call recording is best-effort and depends on device and Android restrictions
