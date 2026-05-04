# Changelog

## [0.4.1] - 2026-04-19

### Added

- ID-based cross-source deduplication for grouping manga across extensions

### Changed

- Simplified UI by removing AGSL shader-based skins and dynamic color extraction
- Restored standard Material 3 components for pull-to-refresh and reader navigation
- Migrated default extension repository to LeverTeam/hikari-extensions
- Updated project documentation for technical accuracy

### Fixed

- Corrected aspect ratio calculation in image decoder to prevent stretched cover images
- Resolved auto-update issues in the application updater

### Removed

- Deleted AGSL shader implementations for Glass, Liquid, and Frosted materials
- Removed dynamic skin color extraction from manga covers
- Disabled the update-checking snackbar on launch and limited automatic check frequency to 12 hours

## [0.3.7] - 2026-04-17

### Added

- Sectional dashboard layout for Advanced, Library, Reader, Browse, Tracking, and Security screens
- High-elevation SectionCard design system with integrated horizontal dividers
- Premium "Welcome Experience" onboarding flow with integrated theme and permission cards
- "Feature Spotlight" dashboard for the New Update screen with contained changelogs
- Unified Security & Locking card with integrated biometrics and privacy controls
- High-performance native NDK-based image decoding system using `AImageDecoder` (API 30+)
- High-performance EASU upscaling filter integrated into the image loading pipeline
- Manual update check trigger in the About screen with reactive status feedback

### Itemization of UI Improvements

- **Settings Modernization**: Restructured all technical preferences into cohesive sectional cards
- **Dashboard Consistency**: Standardized elevation and container styling across the entire app
- **True Transparency**: Achieved clean system bar transparency by removing legacy scrims
- **Navigation Safety**: Optimized WindowInsets handling to prevent UI clipping in true edge-to-edge mode
- **Onboarding Refresh**: Replaced legacy setup lists with interactive configuration dashboards
- **Tracking Refinement**: Grouped all social tracking services into a unified sync dashboard

### Changed

- Refactored settings modules to use a unified card-and-divider design language
- Modernized project README with updated technical specifications and feature roadmap
- Simplified theme widgets by removing legacy BasePreferenceWidget wrappers
- Standardized preference item rendering using unified PreferenceItem composables
- Refactored library and update screens to reactively observe background job status
- Migrated extension manager to a reactive state flow for background fetching
- Centralized WorkManager status observation in LibraryUpdateJob with integrated debounce
- Relaxed background update constraints for extensions to improve reliability on diverse networks
- Enabled lenient JSON parsing in the global network stack for increased API resilience

### Fixed

- Fixed unresolved reference error in SettingsReaderScreen navigation group
- Fixed InjektionException in LibraryScreenModel during context resolution
- Removed legacy hardcoded delays from refresh indicators across the application
- Resolved a race condition where background extension updates could run before initialization
- Fixed persistent JSON parsing crashes in the app update checker
- Fixed NullPointerException in network layer when rebuilding responses with null bodies

### Removed

- Removed legacy hardcoded restrictions (WiFi-only + Charging) for extension auto-updates
- Removed unused LibraryFilterChips component

## [0.3.4] - 2026-04-12

### Added

- GitHub-Style Reading Activity heatmap in Statistics screen with a 365-day scrollable grid
- Interactive tooltips for Reading Activity heatmap with date and chapter count
- High-end parallax transitions and micro-animations for yearly reading statistics
- Integrated social share functionality for generating and sharing yearly summary cards
- "Shareable Card" system for capturing localized reading milestones as high-quality posters

### Fixed

- Resolved timer synchronization and bleed issues in story-style navigation
- Implemented full edge-to-edge support and theme-compliant token usage in Statistics feature

## [0.3.3] - 2026-04-11

### Added

- High-end multi-layered "Liquid" slider for reader progress with sub-surface bubbles and dynamic shimmer
- Global hidden manga list preservation in backups to ensure hidden list is kept across device migrations
- Dynamic palette-aware UI skinning system with AGSL shaders for manga covers
- High-performance native image upscaling using AMD FSR 1.0 (EASU) in the reader
- Asynchronous color extraction for smoother transitions when opening manga details
- Extension auto-update system with WorkManager scheduling and silent background installation support
- Smart chapter merging feature to automatically group decimal and extra chapters in the manga list
- MergedChapterListItem component with folder-style UI and interactive expand/collapse states

### Fixed

- Fixed critical backup issues where library filters, sorting, and extension languages were not being saved
- Fixed initial delay bug in scheduled updates where jobs would wait 24 hours to start if scheduled for the current hour
- Improved auto-update reliability on Android 13+ by simplifying network constraints and VPN handling
- Resolved `ui-graphics` unresolved reference errors in presentation-core
- Fixed Coil 3 migration issues with `asDrawable` conversion for manga covers

## [0.3.2] - 2026-04-10

### Added

- Native C++ image pipeline for reader performance and zero-copy bitmap operations
- AGSL shader-based UI skinning system supporting Glass, Liquid, and Frosted materials
- Global Search Deduplication Engine to group identical manga across multiple sources
- Seamless Intelligent Zoom for Webtoon reader with aspect ratio preservation
- Liquid pull-to-refresh animations with bouncy spring physics
- Staggered "unfolding" entrance animations for manga details screen
- Depth-shift perspective transitions for background elements during navigation
- Immersive shared element transitions for manga covers and titles
- Pulsing elastic heartbeat animation for the refresh indicator
- Reusable spring-scaled pop interactions for navigation items
- Physics-based bounce and squish micro-interactions for library grid and list items

### Changed

- Refactored global search to a reactive streaming multiplexer with automatic result clearing
- Optimized database operations with batching for manga, chapters, and history
- Optimized UpdatesScreenModel to avoid N+1 query patterns during bulk actions
- Modernized coroutine usage across app modules and removed runBlocking from restoration pipelines
- Refactored screen transition engine to use SharedTransitionLayout and AnimatedContentScope

### Fixed

- Resolved binary compatibility issues and NoSuchMethodError in theme engine
- Fixed type mismatches in native image decoder and shader uniform pipelines
- Fixed WorkManager IllegalStateException on app startup
- Fixed NullPointerException in networking layer on empty response bodies
- Optimized haptic feedback to prevent accidental triggers during library scrolling

## [0.1.2] - 2026-04-06

### Added

- Parallel source update concurrency setting to speed up updates for large libraries
- Targeted database indexes for mangas and chapters for significantly improved loading times
- Custom WebtoonImageDecoder for efficient handling of extremely tall vertical content
- Initial Koin modules and setup to begin the architectural migration from Injekt
- **"Seamless Stitch" Webtoon Layout**: Implemented 1-pixel bleeding overlap and disabled clipping for perfect vertical rendering
- **Cronet (HTTP/3) Image Pipeline**: Integrated native Chromium network stack for faster initial image fetching
- **Zero-Copy Image Pipeline**: Optimized `WebtoonPageHolder` to avoid intermediate in-memory buffering, significantly reducing JVM heap pressure
- **Manual WorkManager Initialization**: Fixed startup crashes by implementing custom Configuration.Provider

### Changed

- Migrated SourcePreferencesScreen from legacy Fragment bridge to pure Jetpack Compose
- Updated AniList OAuth to use explicit `redirect_uri` for better browser compatibility
- Refined AniList token expiration calculation and added diagnostic handshake logging

### Fixed

- Resolved AniList login failures where account connection would fail after browser redirect
- Resolved `java.lang.IllegalArgumentException` by ensuring Cronet storage directories are created before engine initialization
- Fixed module-level unresolved reference errors for Cronet in core:common
- Fixed WorkManager IllegalStateException on app startup

## [0.0.2] - 2026-04-04

### Added

- Fixed-hour schedule for library updates and backups

### Changed

- Refactored migration strategy and fixed migration crashes

## [0.0.1] - 2026-04-03

### Added

- Hidden manga management feature in Settings > Browse
- Sources list screen for identifying hidden manga across extensions
- Dedicated grid view for managing hidden manga per source
- Multi-selection mode for batch unhiding manga
- Compact grid layout support with title-on-cover aesthetic for hidden entries
- GetHiddenManga interactor for domain-layer state management
- Database queries for efficient retrieval of hidden manga counts and entries

### Changed

- Refactored hidden manga screens to use project-standard UI tokens and spacing
- Standardized source retrieval using SourceManager to ensure consistent title display
- Optimized hidden manga state management using domain-layer Source models
- Integrated localizable plural strings for accurate hidden manga counts
