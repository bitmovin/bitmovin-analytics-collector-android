# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning](http://semver.org/).

## Development

### Added

- Added Analytics AdAdapter for Bitmovin Player

### Known Issues

- tracking of metrics `percentageInViewport` and `timeInView` not possible in Android 

## v1.9.0
### Fixed
- Error message contained too much data, so that grouping is not useful

### Changed
- Stacktrace is now part of the errorData that is logged, to ease analysis

## Added
- Log unexpected `ExoPlaybackException`s and unchecked exceptions

## v1.8.0

### Added
- Sending out information about selected audio track language and subtitle language for the Bitmovin Player.

## v1.7.0

### Added
- Sending out Device Information about `Build.MANUFACTURER` and `Build.MODEL` as part of the Sample

## v1.6.5

### Changed
- `BitmovinAnalyticsConfig` is parcelable now.
- Android `Context` is now passed seperately where needed.
- Old constructors requiring `Context have been marked as `@Deprecated`

## v1.6.4

### Fixed

- `BitmovinSdkAdapter` didn't transition to the `pause` state on a pause event
- `droppedFrames` was not collected

## v1.6.3

### Fixed

- Fixed a possible NullPointerException if the licensing backend responded with a malformed message that didn't contain a message

## v1.6.2

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
