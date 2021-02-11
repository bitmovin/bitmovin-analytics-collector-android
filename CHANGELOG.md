# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) 
and this project adheres to [Semantic Versioning](http://semver.org/).

## Development

## v1.22.0

### Added

- [Internal] `FeatureManager` allows dynamically adding and disabling features based on player support.
- [Internal] `SegmentTrackingFeature` to always track the latest `n` downloaded elements
- [Internal] `ErrorDetailsFeature` to track additional details in case an error occurs
- [Internal] Added code formatting
- Ability to attach the `ExoPlayerCollector` to a player that's already in a loading state.

## v1.22.0-beta

### Added

- Functionality to retry sending of samples that couldn't be sent due to a HTTP timeout

## v1.21.0

### Added

- ability to override `mpdUrl` and `m3u8Url` with the analytics configuration (AN-1919)

### Changed

- [Internal] refactored `EventData` field population to use an `EventDataManipulationPipeline` (AN-1919)

## v1.20.0

### Added
- If the video startup fails due to a timeout, we add a `ANALYTICS_VIDEOSTART_TIMEOUT_REACHED` error to the sample, so it can be easily filtered in the dashboard

### Changed
- We now stop collecting events after the collector encounters a video startup failure

## v1.19.0

### Changed
- Calculations for available screen width and height to now include screen density and be more independent of physical pixel
- Make collectors more resilient to exceptions in event listeners

## v1.18.0

### Added

- Restricted number of quality changes tracked per period of time (AN-1560)
- Timeout for rebuffering event (AN-1559)

## v1.17.0

### Changed
- tracking of video startup times when having autoplay enabled to be consistent with web platform 

### Fixed
- `CountDownTimer` potentially continues running after the player adapter is released (AN-1595) 

## v1.16.0

### Change

- don't send quality change sample if the quality did not change
- Bitmovin, ExoPlayer: changed player error mapping to improve transparency (AN-1507)
- persist `drmType` in every sample

### Fixed

- bitmovin player crashed when `onAdError` was fired with empty ad tag url (AN-1572)

## v1.15.0

### Added

- `errorData` to samples for bitmovin player (AN-1456)

## v1.14.0

### Added

- `isTV` to sample to distinguish androidTVs from android mobiles
- Playback start failure metrics to ExoPlayer

### Change

- support libraries changed to androidX libraries
- getting audio codec data on BitmovinPlayer
- download speed metric reset

## v1.13.0

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
