<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Lucas Changelog

## [Unreleased]

## [0.5.0]
### Changed
- Updated Lucene to 9.12.0.

### Fixed
- Fixed the collision of Lucene classes coming from the IntelliJ Platform sources and via Lucas plugin dependencies.
This in turn fixes (should fix) the 'Open index' functionality.

## [0.4.1]
### Changed
- Temporarily disabled the Create index menu option.

## [0.4.0]
### Changed
- New supported IDE version range: 2024.2 - 2024.3.*.
- Updated the project to use the IntelliJ Platform Gradle Plugin 2.0.
- Updated the project to use JDK 21.

## [0.3.0]
### Changed
- Updated Lucene to 9.11.1.

## [0.2.0]
### Changed
- Updated Lucene to 9.11.0.
- Changed the file chooser dialog in the *Open Index*, *Create Index* dialogs and the *Custom Analyzer* panel to an IntelliJ one to provide
a smoother user experience.
- Replaced the previous GIF loading icon with a sharper SVG one.

## [0.1.0]
### Added
- Integrated Lucene's Luke application into this IDE plugin. Code changes from Luke are applied up to version 9.10.0.
