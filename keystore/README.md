# Debug keystore

`debug.keystore` here is a **test-only** signing key committed deliberately so every machine signs Vitals debug APKs with the same key. Without this, every local build and every GitHub Actions run would generate a fresh debug key, Android would refuse to install the new APK over the old one (or silently uninstall it), and the user's Room database — which caches Fitbit history — would be wiped each install.

## Credentials (all default Android debug values)

| Field | Value |
|---|---|
| Store password | `android` |
| Key alias | `androiddebugkey` |
| Key password | `android` |
| Validity | 30 years |
| DN | `CN=Vitals Debug, O=Vitals, C=US` |

## Security note

This keystore is for **debug builds only**. It is publicly committed and offers no security — anyone with the repo can sign a debug APK that looks like Vitals. That's fine because:

- Debug-signed APKs cannot be published to the Play Store.
- The Android system requires explicit "Install from unknown source" approval per installer for every debug APK.
- Production releases will use a separate keystore loaded from GitHub Secrets.

Never reuse this keystore for a release build.
