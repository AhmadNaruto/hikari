# Hikari

Manga reader for Android based on Mihon featuring a native image pipeline and sectional UI.

![License](https://img.shields.io/badge/license-Apache_2.0-blue?style=flat-square)
![Version](https://img.shields.io/badge/version-0.3.4-blue?style=flat-square)
![Repo Size](https://img.shields.io/github/repo-size/LeverTeam/hikari?style=flat-square)
![Language](https://img.shields.io/badge/language-Kotlin-purple?style=flat-square)

## Features

- Extension management with automatic sync across multiple repositories
- Sectional dashboard layout for advanced, library, and reader settings
- Native NDK-based image decoding system using AImageDecoder
- High-performance EASU upscaling integrated into the reader pipeline
- Global search deduplication engine for grouping manga across sources
- Material You UI with AGSL shader-based dynamic skinning
- Biometric security dashboard with idle application locking
- Automated library organization and background backup management

## Prerequisites

| Requirement | Version |
| ----------- | ------- |
| JDK         | 21+     |
| Android SDK | API 34+ |
| Gradle      | 8.7+    |

## Installation

### Downloads

Official APK releases are available on the [GitHub Releases](https://github.com/LeverTeam/hikari/releases) page.

### Build

```bash
git clone https://github.com/LeverTeam/hikari.git
cd hikari
./gradlew assembleDebug
```

## Usage

1. Locate the built APK in `app/build/outputs/apk/debug/`
2. Install the APK on an Android device
3. Navigate to Browse > Extensions to initialize extension sources
4. Configure updating and security preferences in the Settings dashboard

## License

Licensed under the [Apache License 2.0](LICENSE).
