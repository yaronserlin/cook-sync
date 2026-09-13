# Media

Images used by the README, the documentation and the public project page.

| Path | Contents |
|---|---|
| `screenshots/` | One image per app screen, named after the screen (`home.jpg`, `cooking-mode.jpg`, `admin-users.jpg`, …). Used by the [user guide](../user-guide.md), the [main README](../../README.md) and the project page. |
| `diagrams/` | `architecture.png` (request path through the system), `erd.jpg` (full database schema) and `screen-flow.png` (navigation between screens). Used by the [functional specification](../functional-spec.md). |
| `apk-qr.png` | QR code for the APK download, used by the README and `index.html`. |

The screenshots and diagrams were taken from the submitted Hebrew documents in
[`../pdf/`](../pdf), so both sets stay in step with each other.

## Adding or replacing a screenshot

Keep the existing file name and the new image drops into every place that
already references it — no markup to update. From a running emulator or device:

```bash
adb exec-out screencap -p > docs/media/screenshots/home.png
```

The current set is 395 × 831 device mockups with the phone frame drawn in and
white rounded corners. The site clips those corners with a `border-radius` in
`assets/site.css` (`.shots img`, `.gallery img`) sized as a percentage, so it
holds at any width. **A plain screenshot with no frame needs that rule relaxed**,
otherwise its own corners get rounded off.

## Adding a demo recording

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
needs regenerating if that URL changes. The ink colour is the app's
`color_accent_900`; keep it dark on white so it stays scannable:

```bash
pip install segno
python3 -c "
import segno
segno.make('https://github.com/yaronserlin/cook-sync/releases/latest/download/CookSync.apk', error='h') \
     .save('docs/media/apk-qr.png', scale=8, border=3, dark='#402310', light='#ffffff')
"
```
