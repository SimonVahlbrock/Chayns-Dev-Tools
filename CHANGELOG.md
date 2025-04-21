<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Chayns-Dev-Tools Changelog

## [Unreleased]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

## [1.4.6]
### Improved
- Migrating from Gradle IntelliJ Plugin (1.x) to (2.x) to cover IDE updates

## [1.4.5]
### Added
- Copy a selected range of a table column
- Button to remove duplicate search results
### Improved
- Share code base of table rendering for sites and persons panel

## [1.4.4]
### Added
- Search for multiple siteIds, locationIds and personIds

## [1.4.3]
### Changed
- Optimise search behaviour by using intellij's search field

## [1.4.2]
### Added
- Create custom persons
### Changed
- Move sync-button to plugin actions

## [1.4.1]
### Added
- Renew renew-token to prevent unnecessary logouts
### Removed
- Api Token Panel (due to security reasons)
- Option to get person info by internal resolve endpoint

## [1.4.0]
### Changed
- Convert code base from kotlin to java

## [1.3.0]
### Added
- Shortcuts to search for persons and sites by selected text
- Load api key credentials by appsettings.json
- Load roles for api key (click to copy)
- Sync saved sites and persons between IDEs
- Use internal user resolve endpoint to search for personId (if api token is provided)
### Changed
- Store renew token instead of user credentials
- Refactor code base for better MVC structure

## [1.2.0]
### Note
- Changes documented in version 1.3.0

## [1.1.0]
### Note
- Changes documented in version 1.3.0

## [1.0.0]
### Added
- Panel for token generation
- Panel to search for sites
- Panel to search for persons