# Spendly Android App Bundle Format (`src/aab/`)

This directory contains the Android App Bundle (`.aab`) packaging metadata and configuration for Google Play Store distribution of **Spendly** (`com.aistudio.spendly.vkmxqr`).

## Generating the `.aab` Bundle
Run Gradle without changing `app/build.gradle.kts` signing or output paths:

```bash
# Debug AAB (signed with debugConfig)
gradle :app:bundleDebug

# Release AAB (requires KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD env vars)
gradle :app:bundleRelease
```

## Standard Output Locations
- **Debug AAB**: `app/build/outputs/bundle/debug/app-debug.aab`
- **Release AAB**: `app/build/outputs/bundle/release/app-release.aab`

## Play Store Optimizations Configured
- `dependenciesInfo { includeInBundle = true }` enabled in `app/build.gradle.kts`
- Vector/adaptive launcher icon (`mipmap-anydpi-v26`) + bundled `.ttf` typography (`inter.ttf`, `plus_jakarta_sans.ttf`, `jetbrains_mono.ttf`)
- ABI & density configuration splitting handled automatically by AGP bundletool.
