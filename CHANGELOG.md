# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning](http://semver.org/).

## Development

### Fixed

- Correct reporting of sources in `BitmovinSdkAdapter` (fields `streamFormat`, `m3u8Url`, `mpdUrl`, `progUrl`)

### Known issues

- Can't get progressive source from `Exoplayer`

## v1.6.1

### Fixed

- `videoTimeStart` and `videoTimeEnd` were not set when sending out heartbeats
- Exoplayer Collector now reports `playerVersion` as `exoplayer-<SDK-version>`
- Bitmovin Collector now reports `playerVersion` as `bitmovin-<SDK-version>`

## v1.6.0

### Added

- `supportedVideoCodecs` in outgoing payload

## v1.5.1

### Fixed

- In the `collector-bitmovin-player`, the `userAgent` contained the player version the collector was compiled against instead of the runtime version

## v1.5.0

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
