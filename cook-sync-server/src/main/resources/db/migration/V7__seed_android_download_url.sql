-- Sets the real download link for the Android build (the rolling GitHub Release published by
-- .github/workflows/onPush.yml on every push to main). Does NOT touch min_supported_version_code
-- — it stays at whatever it already is, so this alone never starts blocking anyone; raising the
-- minimum to actually force an update is a separate, deliberate admin action.
UPDATE app_config
SET download_url = 'https://github.com/yaronserlin/cook-sync/releases/download/latest-apk/app-release.apk',
    updated_at = UTC_TIMESTAMP()
WHERE platform = 'ANDROID';
