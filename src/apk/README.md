# Spendly Android APK Format (`src/apk/`)

Android APK packaging configuration and build guide for **Spendly** (`com.aistudio.spendly.vkmxqr`), built from the live Kotlin & Jetpack Compose Gradle module under `app/`.

## Generating the `.apk` Package
```bash
# Debug APK (signed with debugConfig)
gradle :app:assembleDebug

# Release APK (requires KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD env vars)
gradle :app:assembleRelease
```

## Standard Output Locations
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
