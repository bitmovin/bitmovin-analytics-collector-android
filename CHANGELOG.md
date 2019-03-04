# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning](http://semver.org/).

## v1.4.3

### Added

- `audioCodec` and `videoCodec` in outgoing payload

### Fixed

- Changed `title` property in payload to `videoTitle`
- In some cases the collector reported the player version it was compiled against, not the version in use (both Exoplayer and Bitmovin Player) 

## v1.4.2

### Added

- Logging around LicenseCall responses
- Sequence Number of outgoing Analytics samples
- `platform` = android field in outgoing payload
