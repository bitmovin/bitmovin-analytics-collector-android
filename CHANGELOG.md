# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).
## Development

### Added
- Example app with Amazon IVS setup
- Amazon IVS adapter

### Removed
- [internal] v1 Collectors
- [internal] obsolete test dependencies
- [internal] unnecessary TODOs
- [internal] cleaned obsolete parts of mavenpublish script

### Changed
- [internal] updated gradle plugin and dependencies
- [internal] updated spotless config
- [internal] move to main as default branch
- [internal] refactored PlayerTech to be constant and part of adapters
- [internal] fix AdPosition and AdTagType enums to return configured string representation
- [internal] Converted all Java classes that were left to kotlin
- Upgraded okhttp3 to v4.10.0 (this is a major version upgrade from v3 to v4)
- Upgraded gson dependency to v2.8.9
- Upgraded androidx.appcompat dependency to v1.6.1
- Upgraded example apps to latest bitmovin player (v3.29.0) and latest exoplayer (v2.18.3)
- Deprecated obsolete PlayerType config in AnalyticsConfig
- use `System.getProperty("http.agent")` for user agent tracking
- Made all bitmovin and exoplayer collector classes that are not part of the public API internal

### Fixed
- [internal] reporting of videoTimeStart and videoTimeEnd for audioTrackChange, subtitleChange, qualityChange
- [internal] tracking of http status codes on exoplayer
- Tracking of bitmovin player key when it is specified in the manifest
- Tracking of audiolanguage, audiocodec and videocodec for exoplayer
- Serialization issues when ProGuard is used for obfuscation
- ExoPlayer v2.18.3 could crash on detaching of analytics listener on certain scenarios

## v2.13.0

### Changed
- BitmovinPlayer: Improved ad tracking

### Added
- Detection of FireOS 8

### Fixed
- Detection of UHD screens on AndroidTVs

## v1.36.0

### Changed
- BitmovinPlayer: Improved ad tracking
- Deprecated v1 Collector

### Added
- Detection of FireOS 8

### Fixed
- Detection of UHD screens on AndroidTVs


## v2.12.2

### Changed
- increases error stacktrace size to 50 lines
- [internal] changed from sonarQube to sonarCloud

## v1.35.2
- increases error stacktrace size to 50 lines
- [internal] changed from sonarQube to sonarCloud

## v2.12.1

### Fixed
- Made FeatureManager threadsafe

### Changed
- Upgraded kotlin version to 1.7.20
- Upgraded targetSdkVersion and compileSdkVersion to 33
- Upgraded to gradle 7.3.1
- Updated example apps

## v1.35.1

### Fixed
- Made FeatureManager threadsafe

### Changed
- Upgraded kotlin version to 1.7.20
- Upgraded targetSdkVersion and compileSdkVersion to 33
- Upgraded to gradle 7.3.1
- Updated example apps

## v2.12.0

### Changed
- updates ExoPlayer to `2.18.1`
- updates BitmovinPlayer to `3.25.1`

### Fixed
- improve null checks when extracting Status code from `LoadEventInfo` in `ExoPlayerHttpRequestTrackingAdapter`
- Fixes concurrency bug that could cause issues in certain edge cases where many http requests are sent by the player

## v1.35.0

### Fixed
- improve null checks when extracting Status code from `LoadEventInfo` in `ExoPlayerHttpRequestTrackingAdapter`
- Fixes concurrency bug that could cause issues in certain edge cases where many http requests are sent by the player

## v2.11.0

### Added
- `progUrl` to `BitmovinAnalyticsConfig` and `SourceMetadata`

## v1.34.0

### Added
- `progUrl` to `BitmovinAnalyticsConfig` and `SourceMetadata`

## v2.10.0

### Changed
- updates ExoPlayer to `2.18.x`

### Fixed
- fixed issue where late attaching of collector caused `videoStartupTime` = 0

## v1.33.1

### Fixed
- fixed issue where late attaching of collector caused `videoStartupTime` = 0

## v2.9.0

### Added
- Add exception message to `error_message` field of sample for ExoPlayer collector

## v1.33.0

### Added
- Add exception message to `error_message` field of sample for ExoPlayer collector

## v2.8.0

### Added
- expose used `userId` on Collector
- BitmovinPlayer: if `playerKey` is not set on `BitmovinAnalyticsConfig` we set the value from the player

### Removed
- Heartbeat interval duration configuration on `BitmovinAnalyticsConfig`

### Changed
- [INTERNAL] upgrades gradle and gradle plugin to latest version

## v1.32.0

### Added
- expose used `userId` on Collector
- BitmovinPlayer: if `playerKey` is not set on `BitmovinAnalyticsConfig` we set the value from the player

### Removed
- Heartbeat interval duration configuration on `BitmovinAnalyticsConfig`

### Changed
- [INTERNAL] upgrades gradle and gradle plugin to latest version

## v2.7.1

### Fixed
- displayed wrong screen size for devices with a logical display density != 1

## v1.31.1

### Fixed
- displayed wrong screen size for devices with a logical display density != 1

## v2.7.0

### Changed
- updates ExoPlayer to `2.17.0`

### Removed
- Support for SimpleExoPlayer class

## v1.31.0

### Deprecated
- `ExoPlayerCollector`

## v2.6.2

### Fixed
- Wrong transitions from `startup` state to `paused` state

## v1.30.2

### Fixed
- Wrong transitions from `startup` state to `paused` state

## v2.6.1

### Changed
- improved exception handing on error detail tracking feature

## v1.30.1

### Changed
- improved exception handing on error detail tracking feature

## v2.6.1-beta1

### Changed

- [Internal] Extracted `StateMachineListener` from `BitmovinAnalytics`
- player and kotlin dependency to latest versions

## v1.30.1-beta1

### Changed

- [Internal] Changed gradle player dependencies from `implementation` to `api` so they are included in the published pom file
- player and kotlin dependency to latest versions

## v2.6.0

### Added
- custom data fields 26-30

### Changed
- updates BitmovinPlayer to 3.2.0

## v1.30.0

### Added
- custom data fields 26-30

## v2.5.0

### Added
- Detection of FireOS devices

## v1.29.0

### Added
- Detection of FireOS devices

## v2.4.0

### Added
- `version` property on Collector classes

## v1.28.0

### Added
- `version` property on Collector classes

## v2.3.0

### Added
- customData 8-25 fields

## v1.27.0

### Added
- customData 8-25 fields

## v2.2.0

### Fixed
- BitmovinPlayerCollector didn't report some metrics with sub-second granularity
- ExoPlayerCollector used wrong field to track audioBitrate
- ExoPlayerCollector reported `qualitychange` events although the quality did not change

### Added
- Error detail tracking feature
- `castTech` field
- [Internal] example-shared project

### Removed
- Unintended public methods from Collector classes

## v1.26.0

### Fixed
- BitmovinPlayerCollector didn't report some metrics with sub-second granularity
- ExoPlayerCollector used wrong field to track audioBitrate
- ExoPlayerCollector reported `qualitychange` events although the quality did not change

### Added
- Error detail tracking feature
- `castTech` field
- [Internal] example-shared project

### Removed
- Unintended public methods from Collector classes

## v2.1.0

### Added
- Option to generate randomized `userId` value

## v1.25.0

### Added
- Option to generate randomized `userId` value

## v2.0.0

### Added
- Support for BitmovinPlayer v3
- Support for ExoPlayer version >= 2.12

### Changed
- BitmovinPlayer v3 collector behaviour when using `setCustomData` and `setCustomDataOnce` and custom source configs

## v1.24.0

### Changed

- Support attachment of already playing ExoPlayer to ExoPlayerCollector

## v2.0.0-beta2

### Added
- Support for BitmovinPlayer version >= 3
- `setCustomData` and `setCustomDataOnce`
- [Internal] BitmovinPlayer v3 - handling of multiple sources in PlaylistConfig
- [Internal] Handling of playerStartupTime in cases where the adapter supports multiple sources

### Changed
- Support attachment of already playing ExoPlayer to ExoPlayerCollector
- Added default implementation of AnalyticsListener.onPlayerStateChanged to avoid ExoPlayer crashes on certain devices

## v1.23.0

### Added
- `setCustomData` and `setCustomDataOnce`

### Changed
- Added default implementation of AnalyticsListener.onPlayerStateChanged to avoid ExoPlayer crashes on certain devices

### Removed

## v2.0.0-beta1

### Added
- Support for ExoPlayer version >= 2.12

- [Internal] Separated BitmovinPlayer v2 and v3 collectors and example modules
    - example apps (v1 and latest) package and id renaming
    - example apps (v1 and latest) icons and colors change
- [Internal] Separated exoplayer collector projects
- [Internal] Added new sample app for testing older versions of exoplayer collector
- [Internal] `FeatureManager` allows dynamically adding and disabling features based on player support.
- [Internal] `SegmentTrackingFeature` to always track the latest `n` downloaded elements
- [Internal] `ErrorDetailsFeature` to track additional details in case an error occurs
- [Internal] Enabled code style checks on pre-push and publish
- [Internal] Added new release process for future releases

## v1.22.1

### Fixed

- Added upper exclusive bound of 2.12.0 for ExoPlayer dependency

## v1.22.0

### Added

- Ability to attach the `ExoPlayerCollector` to a player that's already in a loading state.
- [Internal] Added code formatting

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
