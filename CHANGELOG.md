# Changelog

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
