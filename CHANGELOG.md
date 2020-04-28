# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning](http://semver.org/).

## Development

### Added

- heartbeat to Rebuffering state (AN-1281)
- Data downloaded metrics to ExoPlayer

### Fixed

- ExoPlayer: seeking transitioned to rebuffering although no rebuffering state allowed

## v1.12.1

### Fixed

- root Project extension references in gradle files

## v1.12.0

### Added

- `drmLoadTime` and `drmTime` to Sample (ExoPlayer, BitmovinPlayer)

### Fixed

- Clean build.gradle files, fix bitmovinanalyticsexample build error and use project ext props for deps version sharing

## v1.11.3

### Fixed

- collector crash when `isLive` was not set in the analytics config (AN-1092)

## v1.11.2

### Added

- `BitmovinAnalytics.addDebugListener` adds the possibility to listen to debug events

### Fixed

- empty MPD URL when manifest location is null (AN-1068)
- exoplayer demo crash when calling `releasePlayer()`
- Time difference calculation now uses `SystemClock.elapsedRealtime()` to be immune to changes of the phone's date

## v1.11.1

### Changed

- `PlaybackFinished` event will set the `videoTimeEnd` to `videoDuration`, as `currentTime` might not be accurate due to rounding errors

### Fixed

- exoplayer collector crashes when specific exoplayer modules have not been loaded (AN-966)

## v1.11.0

### Added 

- added `customData6` and `customData7` to the EventData and AdEventData

### Fixed

- player started state transition before the player was ready
- pause state activated heartbeat and sent out unnecessary payload 

## v1.10.0

### Added

- Added Analytics AdAdapter for Bitmovin Player
- New boolean configuration field called `isLive` to indicate if the upcoming stream is a live stream. Will be overridden once playback metadata is available.

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
- Old constructors requiring Context have been marked as `@Deprecated`

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
