# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).
## Development

### Changed
- Move from gson to kotlinx.serialization for serialization of DTOs
  (gson is still used for error details in Bitmovin player collector)
- Upgrade gson library to `2.12.1` and appcompat library to `1.7.0`
- Upgraded Bitmovin player to 3.112.0

### v3.14.2

### Fixed
- [AN-4679] Stop sending playing samples when TV is turned off on FireOS 8 devices

### v3.14.1

### Fixed
- Only send one sample in case a startup is interrupted by closing the player (ebvs)
- Implicit detaching of collector when player is attached could create new sessions in rare events

### Changed
- Upgraded to latest ivs player, bitmovin player and media3-exoplayer

### Added
- [Internal] Systemtest for bitmovin collector to cover ebvs when player is closed while startup

### v3.14.0

### Added
- Support for additional customData fields 31-50

## v3.13.0

### Changed
- Use consumer pro guard rules defined in proguard-consumer-rules.pro instead of @Keep to prevent obfuscation of DTOs
- Added explicit runtime dependency to kotlin-parcelize-runtime to fix R8 issues, and changed publish script to also add runtime deps to pom.xml

## v3.12.0

### Fixed
- The StackTrace used for errorDetails didn't include the Exception that was thrown.

### Changed
- Use Throwable.toString() as fallback for the errorMessage in case there is no error message provided
- [Internal] Updated test dependencies

## v3.11.0

### Changed
- Increased targetSdkVersion and compileSdkVersion to 35 (Android 15)
- [Internal] Upgraded agp to 8.7 and gradle to 8.9
- Upgraded bitmovin player to 3.94.0, amazon ivs player to 1.34.0 and media3-exoplayer to 1.5.0
- Handle Bitmovin Decoding ErrorDetails correctly to not cut them off anymore in most cases

## v3.10.1

### Fixed
- Synchronized all methods of ObservableSupport due to crashes seen in android sdk index

### Changed
- [Internal] Enable testreports directly in github ui

## v3.10.0

### Changed
- Tracking of SSAI ad quartiles is off by default and can be enabled via the `ssaiEngagementTrackingEnabled` config in AnalyticsConfig

## v3.9.3

### Fixed
- [AN-4201] Made sequenceNumber and impressionId resetting atomic + refactoring
- CSAI: Added missing pageLoadType = 1 to request (also fixed on server side)

## v3.9.2

### Fixed
- Binary compatibility issues with Bitmovin player bundling that was introduced with `3.9.0` (`AnaltyicsConfig` data class change)

## v3.9.1

### Fixed
- Cache UserId since retrieval is not so cheap can cause the app to hang (as seen in android sdk console logs)
- Make handling of listeners in observable thread safe (seen crashes in android sdk console logs)

## v3.9.0

### Added
- New Config to configure the LogLevel of the SDK (default is LogLevel.ERROR)

## v3.8.1

## Fixed
- All players: Send out sample on detaching of collector to improve data quality

### Changed
- [Internal] Upgraded `bitmovin player` from `3.78.0` to `3.81.0`
- [Internal] Upgraded `media3 exoplayer` from `1.4.0` to `1.4.1`

## v3.8.0

### Added
- New API to report ssai ad quartiles
- [Internal] API checks for the `collector` module.
- [Internal] Github Action's System tests sends a Slack message whenever it fails on the main branch.

### Changed
- Changed `appcompat` version from `1.7.0` to `1.6.1` to have same version as bitmovin player

### Removed
- [Internal] Removed DebugListener related classes from core collector

## v3.7.1

### Fixed
- Enhanced source format and source URL detection for Exoplayer and Media3 ExoPlayer.
- Race condition that could cause a crash in case the ssai API is called before attaching the collector to the player.
- [Internal] Jacoco Coverage do not ignore Robolectric powered tests anymore.

### Changed
- [Internal] Added an hardcoded sequence number limit (1000).
- [Internal] Upgraded `bitmovin player` from `3.75.0` to `3.78.0`
- [Internal] Upgraded `media3 exoplayer` from `1.3.1` to `1.4.0`

## v3.7.0

### Added
- Adding download speed metrics tracking to the Bitmovin Player Collector.

### Fixed
- Removed unnecessary logging of every downloaded file
- Media3 Exoplayer and ExoPlayer: Potential crashes in rare events when DownloadSpeedMeter reports Infinity or NaN values
- [Internal] Fixed the SonarQube test coverage report.

## v3.6.0

### Added
- Adding auto play detection for the Bitmovin Player
- [internal] Added the token for the Google SDK indexing to each of the non-test modules.
- [Internal] Run SystemTests through GitHub Actions on every PR

### Changed
- Upgraded `gson` from `2.10.1` to `2.11.0`
- Upgraded `kotlinx-coroutines` from `1.8.0` to `1.8.1`
- Upgraded `appcompat` from `1.6.1` to `1.7.0`
- [Internal] Upgraded `bitmovin player` from `3.64.0` to `3.75.0`
- [Internal] Upgraded `amazon-ivs` from `1.27.0` to `1.29.0`
- [Internal] Fixed IVS SystemTest setup to be able to run it in CI
- [Internal] The tests samples are now forwarded to the real server.
- [Internal] The tests metadata are now named according to the test name.
- [Internal] Tests that involve blocking actions are now awaited for the result to be available before continuing to increase consistency.
- [Internal] The runBlockingTest function hides it's presence in the stack trace to enhance readability.
- [Internal] The impression id now appears in the error message when a blocking test (which use `runBlockingTest`) fails.
- [Internal] Adding a waiting for all requests mechanism to the tests to ensure that all requests are sent before the test extract the impressions.

## v3.5.1

### Fixed
- Policy violations when using `StrictMode.detectDiskWrites()`, `StrictMode.detectDiskReads()`
  and `StrictMode.detectCustomSlowCalls()`, due to mainScope usage for persistent event queue

### Changed
- [internal] API docs for ssai feature

## v3.5.0

### Added
- new api to flag part of the collected information as ssai stream

## v3.4.0

### Changed
- Increased minimum required compileSdk to 34
- Updated bitmovin player (v3.68.0), media3-exoplayer (v1.3.1) and amazon-ivs (v1.27.0)
- Upgraded kotlin version to 1.9.23
- Upgraded kotlinx-coroutines-android to 1.8.0

### Fixed
- [Internal] Optin for com.bitmovin.player.core.internal.InternalPlayerApi to have access to BuildInfo for bitmovin player version
- [Internal] Updated systemtests for bitmovinplayer to handle newly introduced error codes in player v3.63.0


## v3.3.2

### Fixed
- java.lang.IllegalAccessException when using strict mode with detectIncorrectContextUse()
  due to WindowManager usage

### Changed
- [Internal] Changed Systemtest Managed Devices to use image with API Level 34 and moved
  all except amazon ivs tests to google image since aosp-atd doesn't support drm anymore

## v3.3.1

### Fixed
- Seek events in paused state not tracked correctly (bitmovin, media3, and exoplayer)

## v3.3.0 AND v3.3.0-rc.1

### Changed
- Upgraded to Kotlin 1.9.21
- Upgraded compileSdk and targetSdkVersion to 34
- Upgraded collector dependencies (okhttp3 to 4.12.0, gson to 2.10.1, kotlinx-coroutines-core to 1.7.3)
- Upgraded to latest bitmovin player (v3.54.0)
- [Internal] Upgraded to gradle 8.2
- [Internal] Upgraded test dependencies to latest (mockk to 1.13.7, robolectric to 4.11.1, mockito-inline to 5.2.0,
  ima to 3.31.0, work-runtime-ktx to 2.9.0)
- [Internal] Upgraded build tools (spotless, jacoco)

## v3.2.0

### Added
- IVS Player: Added tracking of source path for ivs player version >= 1.23.0
- All Players: Added tracking of AV1 support on device.

### Changed
- Upgraded to latest bitmovin player (v3.53.0)
- Upgraded to latest amazon ivs player (1.23.0)

## v3.1.0

### Changed
- Changed minimum required android version to android 5.0 (API level 21)
- Upgrade example apps to latest bitmovin player (v3.46.0) and ivs player (v1.22.0)
- ExoPlayer/Media3 ExoPlayer: Use position of event object instead of player.position on seek events

## v3.1.0-beta1

### Added
- Support for media3-exoplayer
- Subtitle tracking for exoplayer

### Changed
- Upgrade example apps to latest bitmovin player (v3.42.0), ivs player (v1.21.0) and exoplayer (v2.19.1)
- [internal] Refactoring of exoplayer collector + increase of testcoverage
- [internal] AN-3404 - Replaced usage of deprecated Handler() with explicit usage of looper (Handler(looper))

## v3.0.1

### Changed
- Upgrade example app to latest bitmovin player (v3.41.0)

### Fixed
- Backwards compatibility for bitmovin player to support versions before 3.39.0

## v3.0.0

### Changed
- Introduced new API v3 for all players with breaking changes compared to v2
- Upgraded to Kotlin 1.8.20
- ExoPlayer: More specific PlaybackException mapping (using errorCodes instead of error types and errorCode names are now part of description)
- Upgrade example app to latest bitmovin player (v3.40.0), latest ivs player (v1.20.0) and latest exoplayer (v2.19.0)
- Bitmovin Player: Throw exception if two bitmovin analytics collector instances are attached to the same player instance
- SourceUrls are autodetected and cannot be specified through metadata anymore

### Fixed
- Prevent context leaking through using context.applicationContext in constructors
- Set current position for videoTimeStart and videoTimeEnd when customData event is sent
- IVS Player: Ignore isLive property for IVS player since it is just a fallback in case the player
  does not provide the information and IVS player doesn't differentiate between
  live and source not loaded, so it cannot be used there. This streamlines the behaviour with other players.
- ExoPlayer: More reliable tracking if player is muted

## v2.18.0

### Changed
- Upgrade example app to latest bitmovin player (v3.38.0) and latest exoplayer version (v2.18.7)

### Added
- `CollectorConfig.longTermRetryEnabled` with default value of `false`. When set to `true`, analytics events that have failed to be sent are cached in a persistent way and re-sent as soon as there is network again

## v2.17.0

### Changed
- [internal] Bitmovin player collector depends on `player-core` package instead of `player`
- All collectors use `compileOnly` dependency to players instead of `implementation`
- Upgrade example app to latest bitmovin player (v3.37.0)
- Upgrade example app to latest exoplayer version (v2.18.6)
- IVS Player and ExoPlayer: Streamlined reporting of video duration for live streams to be 0, to be consistent with other players/platforms

### Added
- [internal] New `AnalyticsConfig` which can be used to create a `AnalyticsCollector`

## v2.16.0

### Changed
- Upgrade example apps to use latest amazon ivs player (v1.18.0) and latest bitmovin player (v3.35.2)

### Added
- [internal] Systemtests for Bitmovin and Exoplayer collectors

### Fixed
- Bitmovin Player: New quality on quality change events was tracked one sample too early
- IVS Player: Wrong initialization order on startup
- IVS Player: Wrong order of releasing of resources when collector is detached
- IVS Player: Reporting of negative droppedFrames in certain edgecases
- Exo Player and Bitmovin Player: Tracking if player is muted
- All players: Reporting of `videostartup_time = 0` on certain edgecases where startup was very fast

## v2.15.0

### Changed
- [internal] Bitmovin player version detection catches `ClassNotFoundException` and fallbacks to core module BuildConfig.
- Upgraded example apps to latest bitmovin player (v3.35.1) and latest exoplayer (v2.18.5)

### Added
- [internal] Systemtests for IVS collector

### Fixed
- Calculation of dropped frames on IVS collector
- Workaround issue in Bitmovin Player version 3.34.0 and 3.35.0 that would lead to a
    `ClassNotFoundException` when detecting the player version.

## v2.14.0

### Added
- Example app with Amazon IVS setup
- Amazon IVS adapter
- Interfaces for each collector
- Descriptive error messages for analytics errors

### Removed
- [internal] obsolete test dependencies
- [internal] unnecessary TODOs
- [internal] cleaned obsolete parts of mavenpublish script
- Deprecated v1 Collectors

### Changed
- [internal] updated gradle plugin and build dependencies
- [internal] updated spotless config
- [internal] move to main as default branch
- [internal] refactored PlayerTech to be constant and part of adapters
- [internal] fix AdPosition and AdTagType enums to return configured string representation
- [internal] Converted all Java classes that were left to kotlin
- [internal] cleanup of exoplayer example app and added more scenarios
- [internal] use `System.getProperty("http.agent")` for user agent tracking
- Upgraded okhttp3 to v4.10.0 (this is a major version upgrade from v3 to v4)
- Upgraded gson dependency to v2.8.9
- Upgraded androidx.appcompat dependency to v1.6.1
- Upgraded example apps to latest bitmovin player (v3.33.0) and latest exoplayer (v2.18.4)
- Deprecated obsolete PlayerType config in AnalyticsConfig
- Made all bitmovin and exoplayer collector classes that are not part of the public API internal
- Stopped sending samples once player is released and collector is still attached

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
