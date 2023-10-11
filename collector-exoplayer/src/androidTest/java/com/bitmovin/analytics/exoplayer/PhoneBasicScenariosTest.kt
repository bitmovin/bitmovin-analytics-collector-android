package com.bitmovin.analytics.exoplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.TestSources.DASH_SINTEL_WITH_SUBTITLES
import com.bitmovin.analytics.systemtest.utils.TestSources.HLS_MULTIPLE_AUDIO_LANGUAGES
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.DrmConfiguration
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using exoplayer player
// This tests assume a phone with api level 30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultSample = TestSources.HLS_REDBULL
    private val defaultMediaItem = MediaItem.fromUri(defaultSample.m3u8Url!!)
    private val defaultSourceMetadata = SourceMetadata(
        title = "hls_redbull",
        videoId = "hls_redbull_id",
        path = "hls_redbull_path",
        customData = TestConfig.createDummyCustomData(),
        cdnProvider = "cdn_provider",
    )

    private val forceLowestQuality = TrackSelectionParameters.Builder()
        .setForceLowestBitrate(true)
        .build()

    private lateinit var player: ExoPlayer
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
        player = ExoPlayer.Builder(appContext).build()
    }

    @After
    fun teardown() {
        MockedIngress.stopServer()
    }

    @Test
    fun test_vodHls_playPauseWithPlayWhenReady() {
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            player.volume = 0.0f
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
        }

        // we wait until player is in ready state before we call play to test this specific scenario
        ExoPlayerPlaybackUtils.waitUntilPlayerIsReady(player)

        mainScope.launch {
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 500)

        mainScope.launch {
            player.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            player.play()
        }

        // we sleep a bit longer to increase probability of a qualitychange event
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 10000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions).hasSize(1)

        val impression = impressions.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList

        DataVerifier.verifyStaticData(eventDataList, defaultSourceMetadata, defaultSample, ExoplayerConstants.playerInfo)
        DataVerifier.verifyM3u8SourceUrl(eventDataList, defaultSample.m3u8Url!!)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyInvariants(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
    }

    @Test
    fun test_vodDash_playPauseWithPlayWhenReady() {
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        val dashSource = TestSources.DASH
        val dashMediaItem = MediaItem.fromUri(dashSource.mpdUrl!!)

        // act
        mainScope.launch {
            player.volume = 0.0f
            collector.attachPlayer(player)
            player.setMediaItem(dashMediaItem)
            player.prepare()
        }

        // we wait until player is in ready state before we call play to test this specific scenario
        ExoPlayerPlaybackUtils.waitUntilPlayerIsReady(player)

        mainScope.launch {
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 500)

        mainScope.launch {
            player.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            player.play()
        }

        // we sleep a bit longer to increase probability of a qualitychange event
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions).hasSize(1)

        val impression = impressions.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList

        DataVerifier.verifyStaticData(eventDataList, SourceMetadata(), dashSource, ExoplayerConstants.playerInfo)
        DataVerifier.verifyMpdSourceUrl(eventDataList, dashSource.mpdUrl!!)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyInvariants(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
    }

    @Test
    fun test_sendCustomDataEvent() {
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata
        val customData1 = TestConfig.createDummyCustomData("customData1")
        val customData2 = TestConfig.createDummyCustomData("customData2")
        val customData3 = TestConfig.createDummyCustomData("customData3")
        val customData4 = TestConfig.createDummyCustomData("customData4")
        val customData5 = TestConfig.createDummyCustomData("customData5")
        // act
        mainScope.launch {
            collector.sendCustomDataEvent(customData1) // since we are not attached this shouldn't be sent
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerIsReady(player)

        mainScope.launch {
            collector.sendCustomDataEvent(customData2)
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2001)

        mainScope.launch {
            collector.sendCustomDataEvent(customData3)
            player.pause()
            collector.sendCustomDataEvent(customData4)
            collector.detachPlayer()
            player.release()
            collector.sendCustomDataEvent(customData5) // this event should not be sent since collector is detached
        }

        Thread.sleep(300)

        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions).hasSize(1)

        val impression = impressions.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList

        val customDataEvents = eventDataList.filter { it.state == "customdatachange" }.toMutableList()
        val nonCustomDataEvents = eventDataList.filter { it.state != "customdatachange" }.toMutableList()
        DataVerifier.verifyM3u8SourceUrl(nonCustomDataEvents, defaultSample.m3u8Url!!)
        DataVerifier.verifyM3u8SourceUrl(customDataEvents, defaultSample.m3u8Url!!)

        assertThat(customDataEvents).hasSize(3)
        DataVerifier.verifySourceMetadata(customDataEvents[0], defaultSourceMetadata.copy(customData = customData2))
        assertThat(customDataEvents[0].videoTimeStart).isEqualTo(0)
        assertThat(customDataEvents[0].videoTimeEnd).isEqualTo(0)

        DataVerifier.verifySourceMetadata(customDataEvents[1], defaultSourceMetadata.copy(customData = customData3))
        assertThat(customDataEvents[1].videoTimeStart).isNotEqualTo(0)
        assertThat(customDataEvents[1].videoTimeEnd).isNotEqualTo(0)

        DataVerifier.verifySourceMetadata(customDataEvents[2], defaultSourceMetadata.copy(customData = customData4))
        assertThat(customDataEvents[2].videoTimeStart).isGreaterThan(2000)
        assertThat(customDataEvents[2].videoTimeEnd).isGreaterThan(2000)
    }

    @Test
    fun test_live_playWithAutoplay() {
        // arrange
        val liveSample = TestSources.DASH_LIVE
        val liveSource = MediaItem.fromUri(liveSample.mpdUrl!!)
        val liveSourceMetadata = SourceMetadata(title = "liveSource", videoId = "liveSourceId", cdnProvider = "cdn_provider", customData = TestConfig.createDummyCustomData(), isLive = true)

        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.playWhenReady = true
            player.setMediaItem(liveSource)
            collector.sourceMetadata = liveSourceMetadata
            player.prepare()
        }
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
        }

        // wait a bit to make sure last play sample is sent
        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList).hasSize(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, liveSourceMetadata, liveSample, ExoplayerConstants.playerInfo)
        DataVerifier.verifyMpdSourceUrl(eventDataList, liveSample.mpdUrl!!)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(false))
        DataVerifier.verifyInvariants(eventDataList)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        // verify that no other states than startup and playing were reached
        assertThat(eventDataList.filter { x -> x.state != "startup" && x.state != "playing" })
            .hasSize(0)
    }

    @Test
    fun test_vodDashWithDrmWidevine_playWithAutoPlay() {
        // arrange
        val sample = TestSources.DRM_DASH_WIDEVINE
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        val mediaItem = MediaItem.Builder()
            .setDrmConfiguration(
                DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(sample.drmLicenseUrl)
                    .build(),
            )
            .setUri(sample.mpdUrl)
            .build()
        val drmSourceMetadata = SourceMetadata(title = "drmTest", videoId = "drmTest", cdnProvider = "cdn_provider", customData = TestConfig.createDummyCustomData())

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.playWhenReady = true
            player.setMediaItem(mediaItem)
            collector.sourceMetadata = drmSourceMetadata
            player.prepare()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

        mainScope.launch {
            player.pause()
        }

        Thread.sleep(300)
        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions).hasSize(1)

        val drmImpression = impressions[0]
        DataVerifier.verifyHasNoErrorSamples(drmImpression)
        val startupSample = drmImpression.eventDataList.first()
        DataVerifier.verifyDrmStartupSample(startupSample, sample.drmSchema)
    }

    @Test
    fun test_vodDashWithDrmWidevine_playWithoutAutoplay() {
        // arrange
        val sample = TestSources.DRM_DASH_WIDEVINE
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        val mediaItem = MediaItem.Builder()
            .setDrmConfiguration(
                DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(sample.drmLicenseUrl)
                    .build(),
            )
            .setUri(sample.mpdUrl)
            .build()
        val drmSourceMetadata = SourceMetadata(title = "drmTest", videoId = "drmTest", cdnProvider = "cdn_provider", customData = TestConfig.createDummyCustomData())

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(mediaItem)
            collector.sourceMetadata = drmSourceMetadata
            player.prepare()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerIsReady(player)

        mainScope.launch {
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

        mainScope.launch {
            player.pause()
        }

        Thread.sleep(300)
        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions).hasSize(1)

        val drmImpression = impressions[0]
        DataVerifier.verifyHasNoErrorSamples(drmImpression)
        val startupSample = drmImpression.eventDataList.first()
        DataVerifier.verifyDrmStartupSample(startupSample, sample.drmSchema, isAutoPlay = false)
    }

    @Test
    fun test_vod_2Impressions_shouldReportSourceMetadataCorrectly() {
        // first dash impression
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        val dashSource = TestSources.DASH
        val dashMediaItem = MediaItem.fromUri(dashSource.mpdUrl!!)
        val dashSourceMetadata = SourceMetadata(title = "dashSource", videoId = "dashSourceId", cdnProvider = "cdn_provider", customData = TestConfig.createDummyCustomData())

        // loading dash source
        mainScope.launch {
            player.volume = 0.0f
            player.playWhenReady = true
            collector.sourceMetadata = dashSourceMetadata
            collector.attachPlayer(player)
            player.setMediaItem(dashMediaItem)
            player.trackSelectionParameters = forceLowestQuality
            player.prepare()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

        // loading hls source
        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.stop()
            player.seekTo(0)

            collector.sourceMetadata = defaultSourceMetadata
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.play()
        }

        // wait 1 second to make sure the new media item is loaded
        Thread.sleep(1000)
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions).hasSize(2)

        val dashImpression = impressions[0]
        DataVerifier.verifyHasNoErrorSamples(dashImpression)
        DataVerifier.verifyStartupSample(dashImpression.eventDataList[0])
        DataVerifier.verifyMpdSourceUrl(dashImpression.eventDataList, dashSource.mpdUrl!!)
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(dashImpression.eventDataList)

        val hlsImpression = impressions[1]
        DataVerifier.verifyHasNoErrorSamples(hlsImpression)
        DataVerifier.verifyStartupSample(hlsImpression.eventDataList[0], isFirstImpression = false)
        DataVerifier.verifyM3u8SourceUrl(hlsImpression.eventDataList, defaultSample.m3u8Url!!)
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(hlsImpression.eventDataList)
    }

    @Test
    fun test_vodHls_seekForwardsAndBackwards() {
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            player.volume = 0.0f
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.trackSelectionParameters = forceLowestQuality
            player.prepare()
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

        mainScope.launch {
            player.pause()
            player.seekTo(10000)
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 12000)

        mainScope.launch {
            player.seekTo(3000)
        }

        Thread.sleep(500)
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)
        val impressionsList = MockedIngress.extractImpressions()
        assertThat(impressionsList).hasSize(1)

        val impression = impressionsList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val seeks = impression.eventDataList.filter { it.state == DataVerifier.SEEKING }
        assertThat(seeks).hasSize(2)

        val forwardSeek = seeks[0]
        val backwardSeek = seeks[1]

        DataVerifier.verifyForwardsSeek(forwardSeek)
        DataVerifier.verifyBackwardsSeek(backwardSeek)
    }

    @Test
    fun test_vod_playWithLowestQuality_ShouldUsePlayingHeartbeat() {
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            player.volume = 0.0f
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.trackSelectionParameters = forceLowestQuality
            player.play()
        }

        // we wait for at least 80000 seconds to make sure we get
        // 1 heartbeat (~60seconds) during playing
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 20000)
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 40000)
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 60000)
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 80000)

        mainScope.launch {
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions).hasSize(1)

        val impression = impressions.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        // since use lowest quality, we expect only 1 startup and 1 playing sample
        // (using lowest quality doesn't trigger qualitychange events)
        assertThat(impression.eventDataList).hasSize(2)
        DataVerifier.verifyStartupSample(impression.eventDataList[0])

        // second sample is playing sample triggered through playing heartbeat
        val secondSample = impression.eventDataList[1]
        assertThat(secondSample.state).isEqualTo(DataVerifier.PLAYING)
        assertThat(secondSample.played).isGreaterThan(55000L)
    }

    @Test
    fun test_vod_enableSwitchAndDisableSubtitles() {
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = SourceMetadata(title = "sintel_with_subtitles")
        val mediaItem = MediaItem.fromUri(DASH_SINTEL_WITH_SUBTITLES.mpdUrl!!)

        // act
        val preferEnglishSubtitle = TrackSelectionParameters.Builder()
            .setForceLowestBitrate(true)
            .setPreferredTextLanguage("de")
            .build()

        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(mediaItem)

            // select english as preferred subtitle
            player.trackSelectionParameters = preferEnglishSubtitle

            player.prepare()
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

        val preferGermanSubtitle = TrackSelectionParameters.Builder()
            .setForceLowestBitrate(true)
            .setPreferredTextLanguage("en")
            .build()

        mainScope.launch {
            // select german as preferred subtitle
            player.trackSelectionParameters = preferGermanSubtitle
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

        val disableTextTrack = TrackSelectionParameters.Builder()
            .setForceLowestBitrate(true)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()

        mainScope.launch {
            player.pause()

            // disable subtitles
            player.trackSelectionParameters = disableTextTrack
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 10000)

        mainScope.launch {
            player.pause()
            player.play()
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        val impressionsList = MockedIngress.extractImpressions()
        assertThat(impressionsList).hasSize(1)

        val impression = impressionsList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val subtitleEnabledSamples = impression.eventDataList.filter { it.subtitleEnabled }
        val subtitleDisabledSamples = impression.eventDataList.filter { !it.subtitleEnabled && it.subtitleLanguage == null }

        val germanSubtitleSamples = subtitleEnabledSamples.filter { it.subtitleLanguage == "de" }
        val englishSubtitleSamples = subtitleEnabledSamples.filter { it.subtitleLanguage == "en" }

        assertThat(germanSubtitleSamples).hasSizeGreaterThanOrEqualTo(1)
        assertThat(englishSubtitleSamples).hasSizeGreaterThanOrEqualTo(1)

        assertThat(subtitleDisabledSamples).hasSizeGreaterThanOrEqualTo(1)

        // sanity check that samples count adds up
        assertThat(impression.eventDataList.size).isEqualTo(englishSubtitleSamples.size + germanSubtitleSamples.size + subtitleDisabledSamples.size)
    }

    @Test
    fun test_vod_switchAudioLanguageTrack() {
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = SourceMetadata(title = "different_languages_test")
        val mediaItem = MediaItem.fromUri(HLS_MULTIPLE_AUDIO_LANGUAGES.m3u8Url!!)

        val preferDubbingAudio = TrackSelectionParameters.Builder()
            .setForceLowestBitrate(true)
            .setPreferredAudioLanguage("dubbing")
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()

        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(mediaItem)

            // select dubbing as preferred language
            player.trackSelectionParameters = preferDubbingAudio

            player.prepare()
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        val preferEnglishAudio = TrackSelectionParameters.Builder()
            .setForceLowestBitrate(true)
            .setPreferredAudioLanguage("en")
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()

        mainScope.launch {
            // switch to english audio
            player.trackSelectionParameters = preferEnglishAudio
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList).hasSize(1)

        val impression = impressionList.first()
        val dubbingSamples = impression.eventDataList.filter { it.audioLanguage == "dubbing" }
        val englishSamples = impression.eventDataList.filter { it.audioLanguage == "en" }

        assertThat(dubbingSamples).hasSizeGreaterThanOrEqualTo(2)
        assertThat(englishSamples).hasSizeGreaterThanOrEqualTo(1)
    }

    @Test
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() {
        val sample = Samples.HLS_REDBULL
        val analyticsConfig = TestConfig.createAnalyticsConfig("nonExistingKey", backendUrl = mockedIngressUrl)
        val collector = IExoPlayerCollector.create(appContext, analyticsConfig)

        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(MediaItem.fromUri(sample.uri))
            player.prepare()
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
            assertThat(player.currentPosition).isGreaterThan(1000)
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        // assert that no samples are sent
        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions).hasSize(0)
    }
}
