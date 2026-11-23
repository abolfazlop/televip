# TeleVIP — Abolfazl TeleX VIP

A modular Android Xposed project for Telegram.

## Target device
- POCO X7 Pro
- Android 16 (API 36)
- LSPosed 2.2.1 / Xposed API 102 (reported by device)
- Telegram package: `org.telegram.messenger`
- Telegram version reported: 13.3.1
- App ID: `com.abolfazl.telexvip`

## Current status
This is the initial diagnostic bootstrap. It registers a libxposed entry point, declares Telegram as the static scope, and logs when the module and Telegram package load. It does not yet modify Telegram behavior; feature hooks will be implemented and tested independently.

## Build
Open in Android Studio with JDK 17+ and Android SDK Platform 35 installed. Sync Gradle and run `:app:assembleDebug`.

## Roadmap
- Core module loader and compatibility diagnostics
- Privacy controls
- Message metadata and identifiers
- Ad filtering
- Stories and media utilities
- Appearance customization
- User-defined extension modules

Only independently implemented, permission-respecting features are planned. Protected-content bypasses and unauthorized Premium unlocking are excluded.
