# Hikari

Manga reader for Android based on Mihon with NDK image decoding and sectional UI.

![License](https://img.shields.io/badge/license-Apache_2.0-blue?style=flat-square)
![Version](https://img.shields.io/badge/version-0.5.2-blue?style=flat-square)
![Repo Size](https://img.shields.io/github/repo-size/LeverTeam/hikari?style=flat-square)
![Language](https://img.shields.io/badge/language-Kotlin-purple?style=flat-square)

## Features

- Redesigned extension and source filtering with local search, card-grouping panels, custom flag badges, and active source count tracking
- Redesigned onboarding setup flow with automated theme selections and system permission configuration dashboards
- Unified card, list item, and detail screen containers under the custom HikariCard component
- Rebuilt Compose-based download queue with real-time active downloader pipeline
- Extension management with automatic repository synchronization
- Sectional dashboard for library and reader configuration
- NDK-based image decoding using AImageDecoder
- EASU upscaling integration in the reader pipeline
- Global search deduplication for cross-source manga grouping
- Material 3 user interface with Material You support
- Biometric security with idle application locking
- Background library organization and backup management

## Prerequisites

| Requirement | Version |
| ----------- | ------- |
| JDK         | 21+     |
| Android SDK | API 34+ |
| Gradle      | 8.7+    |

## Installation

Build from source:

```bash
git clone https://github.com/LeverTeam/hikari.git
cd hikari
./gradlew assembleDebug
```

Official APK releases are available on the [GitHub Releases](https://github.com/LeverTeam/hikari/releases) page.

## Usage

1. Locate the built APK in `app/build/outputs/apk/debug/`
2. Install the APK on an Android device
3. Navigate to Browse > Extensions to initialize extension sources
4. Configure updating and security preferences in the Settings dashboard

## License

Licensed under the [Apache License 2.0](LICENSE).
