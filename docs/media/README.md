# Media

Images used by the README, the documentation and the public project page.

| File | Used by |
|---|---|
| `apk-qr.png` | [Main README](../../README.md) and [`index.html`](../index.html) — points at the latest-APK download URL, so it never needs regenerating |

## Adding screenshots

Screenshots are the highest-value thing that can be added to this repository:
they work with no network, no server and no install, and they are the first
thing a visitor looks at.

Capture them at device resolution and drop them here as
`screenshot-<screen>.png` — for example `screenshot-home.png`,
`screenshot-recipe.png`, `screenshot-cooking-mode.png`, `screenshot-wizard.png`,
`screenshot-admin.png`. Then uncomment the screenshot table in the
[main README](../../README.md).

From a running emulator or device:

```bash
adb exec-out screencap -p > docs/media/screenshot-home.png
```

## Adding a demo recording

A short screen recording of one complete flow (search → recipe → Cooking Mode)
carries a presentation better than any static image.

```bash
adb shell screenrecord --time-limit 30 /sdcard/demo.mp4
adb pull /sdcard/demo.mp4 docs/media/demo.mp4
```

Keep an animated GIF under about 5 MB so it loads inline on GitHub; anything
longer or larger belongs in an MP4, which GitHub plays inline in a README when
it is committed to the repository.

## Regenerating the QR code

The QR encodes
`https://github.com/yaronserlin/cook-sync/releases/latest/download/CookSync.apk`
— a permanent URL that always resolves to the newest published APK — so it only
needs regenerating if that URL changes:

```bash
pip install segno
python3 -c "
import segno
segno.make('https://github.com/yaronserlin/cook-sync/releases/latest/download/CookSync.apk', error='h') \
     .save('docs/media/apk-qr.png', scale=8, border=3, dark='#2a6f4b', light='#ffffff')
"
```
