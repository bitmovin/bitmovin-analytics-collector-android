package com.bitmovin.analytics.theoplayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.RepeatRule
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.TestSources.DASH_SINTEL_WITH_SUBTITLES
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.PreloadType
import com.theoplayer.android.api.player.track.texttrack.TextTrackMode
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("ktlint:standard:max-line-length")
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val theoPlayerAsset = TestSources.THEO_BIGBUCKBUNNY

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    @Rule @JvmField
    val repeatRule = RepeatRule()

    // Source metadata title depends on the test, so it has to be generated dynamically
    private var defaultSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                videoId = "theoPlayerSample",
                path = "theoPlayerSamplePath",
                customData = TestConfig.createDummyCustomData(),
                cdnProvider = "cdn_provider",
            )
        set(_) {}

    private var redBullHlsSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                videoId = "redBullHls",
                path = "redBullHlsPath",
                customData = TestConfig.createDummyCustomData(),
                cdnProvider = "cdn_provider",
            )
        set(_) {}

    private var liveSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                videoId = "live",
                path = "live",
                customData = TestConfig.createDummyCustomData(),
                cdnProvider = "cdn_provider",
            )
        set(_) {}

    private lateinit var player: Player
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    private lateinit var theoPlayerView: THEOplayerView

    private val defaultDashSource =
        TypedSource
            .Builder(theoPlayerAsset.mpdUrl!!)
            .type(SourceType.DASH)
            .build()

    private val defaultDashSourceDescription =
        SourceDescription
            .Builder(defaultDashSource)
            .build()

    private val hlsRedbullSource =
        TypedSource
            .Builder(TestSources.HLS_REDBULL.m3u8Url!!)
            .type(SourceType.HLS)
            .build()
    private val hlsRedBullSourceDescription =
        SourceDescription
            .Builder(hlsRedbullSource)
            .build()

    private val liveSource =
        TypedSource
            .Builder(TestSources.DASH_LIVE.mpdUrl!!)
            .type(SourceType.DASH)
            .build()
    private val liveSourceDescription =
        SourceDescription
            .Builder(liveSource)
            .build()

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)

        val playerConfig =
            THEOplayerConfig.Builder()
                .license(TheoPlayerTestUtils.TESTING_LICENSE) // empty license for testing
                .build()

        runBlocking {
            withContext(mainScope.coroutineContext) {
                theoPlayerView = THEOplayerView(appContext, playerConfig)
                player = theoPlayerView.player
            }
        }
    }

    @After
    fun teardown() {
        runBlocking {
            withContext(mainScope.coroutineContext) {
                if (!theoPlayerView.isDestroyed) {
                    theoPlayerView.onDestroy()
                }
            }
        }

        MockedIngress.stopServer()
    }

    @Test
    fun test_vodDash_playPausePlayWithAutoPlayAndMuted() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                player.volume = 0.0
                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = defaultDashSourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                player.play()
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            // expecting exactly 4 samples given that we force lowest rendition
            // startup -> play -> pause -> play
            assertThat(eventDataList).hasSize(4)
            val startupSample = eventDataList.first()
            val playingSample = eventDataList[1]

            DataVerifier.verifyStaticData(
                eventDataList,
                defaultSourceMetadata,
                TestSources.THEO_BIGBUCKBUNNY,
                TheoPlayerConstants.playerInfo,
                false,
            )
            DataVerifier.verifyStartupSample(startupSample)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(isMuted = true, isAutoPlayEnabled = true))
            DataVerifier.verifyIsPlayingEvent(playingSample)
            DataVerifier.verifyInvariants(eventDataList)
        }
    }

    @Test
    fun test_liveStream_playPause() {
        runBlockingTest {
            val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                collector.sourceMetadata = liveSourceMetadata
                collector.attachPlayer(player)
                player.source = liveSourceDescription
            }

            Thread.sleep(6000)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            Thread.sleep(200)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            // expecting at least two samples samples given that we force lowest rendition
            // startup -> play
            assertThat(eventDataList).hasSizeGreaterThanOrEqualTo(2)
            val startupSample = eventDataList.first()
            val playingSample = eventDataList[1]

            DataVerifier.verifyStaticData(
                eventDataList,
                liveSourceMetadata,
                TestSources.DASH_LIVE,
                TheoPlayerConstants.playerInfo,
                false,
            )
            DataVerifier.verifyStartupSample(startupSample)
            DataVerifier.verifyIsPlayingEvent(playingSample)
            DataVerifier.verifyInvariants(eventDataList)
            DataVerifier.verifyIsLiveIsConsistentlySet(eventDataList, true)
        }
    }

    @Test
    fun test_vodDash_playPauseWithoutAutoplay() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.preload = PreloadType.AUTO
                player.source = defaultDashSourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerIsReady(player)

            withContext(mainScope.coroutineContext) {
                player.play()
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            // expecting exactly two samples given that we force lowest rendition
            assertThat(eventDataList).hasSize(2)
            val startupSample = eventDataList.first()
            val playingSample = eventDataList[1]

            DataVerifier.verifyStartupSample(startupSample)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(isMuted = false, isAutoPlayEnabled = false))
            DataVerifier.verifyIsPlayingEvent(playingSample)
            DataVerifier.verifyInvariants(eventDataList)
        }
    }

    @Test
    fun test_vodDash_playSeekForwardPlay() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = defaultDashSourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                // seek
                player.currentTime = 5.00
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            // we should have start, play, seek, play
            assertThat(eventDataList).hasSizeGreaterThanOrEqualTo(4)
            val startupSample = eventDataList.first()
            val playingSample = eventDataList[1]

            DataVerifier.verifyStartupSample(startupSample)
            DataVerifier.verifyIsPlayingEvent(playingSample)

            // TODO: disabled since seeking videotime tracking is not working currently
//            DataVerifier.verifyInvariants(eventDataList)
        }
    }

    @Test
    fun test_vodDash_seekWhilePaused() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = defaultDashSourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 500)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(100)

            withContext(mainScope.coroutineContext) {
                player.currentTime = 3.0
            }

            Thread.sleep(2000)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            // we should have start, play, pause, seek
            assertThat(eventDataList).hasSizeGreaterThanOrEqualTo(4)
            val startupSample = eventDataList.first()
            val playingSample = eventDataList[1]

            DataVerifier.verifyStartupSample(startupSample)
            DataVerifier.verifyIsPlayingEvent(playingSample)
            DataVerifier.verifyInvariants(eventDataList)
        }
    }

    @Test
    fun test_vodDash_playSeekBackwardPlay() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = defaultDashSourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                // seek
                player.currentTime = 1.00
            }

            Thread.sleep(500)
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            // we should have start, play, seek, play
            assertThat(eventDataList).hasSizeGreaterThanOrEqualTo(4)
            val startupSample = eventDataList.first()
            val playingSample = eventDataList[1]

            DataVerifier.verifyStartupSample(startupSample)
            DataVerifier.verifyIsPlayingEvent(playingSample)
            // TODO: disabled since seeking videotime tracking is not working currently
//            DataVerifier.verifyInvariants(eventDataList)
        }
    }

    @Test
    fun test_vodDash_playQualityChangePlay() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.useLowestRendition()
                player.source = defaultDashSourceDescription
                player.play()
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                // switch to highest quality
                val desiredTrack = player.videoTracks.first()
                desiredTrack?.let { track ->
                    val minQuality = track.qualities.maxBy { quality -> quality.bandwidth }
                    track.targetQuality = minQuality
                }
                player.play()
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            // TODO: verify if buffering or quality change events are happening
            // we expect startup, playing, qualitychange/buffering, playing
            assertThat(eventDataList).hasSizeGreaterThanOrEqualTo(4)
            val startupSample = eventDataList.first()
            val playingSample = eventDataList[1]

            DataVerifier.verifyStartupSample(startupSample)
            DataVerifier.verifyIsPlayingEvent(playingSample)
            DataVerifier.verifyInvariants(eventDataList)
        }
    }

    @Test
    fun test_vodDash_sendLastSampleOnDestroy() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                player.isAutoplay = true
                player.useLowestRendition()
                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = defaultDashSourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                theoPlayerView.onDestroy()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            // we expect exactly 1 startup and 1 playing sample
            assertThat(eventDataList).hasSize(2)
        }
    }

    @Test
    fun test_vod_sourceChangeWithDetachAndAttach() {
        runBlockingTest {
            val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = defaultDashSourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                // switch source
                collector.detachPlayer()
                collector.sourceMetadata = redBullHlsSourceMetadata
                player.source = hlsRedBullSourceDescription

                // TODO: seems like it doesn't matter if we attach after or before the source change
                // something to be verified
                collector.attachPlayer(player)
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                theoPlayerView.onDestroy()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(2)

            val firstImpression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(firstImpression)
            val secondImpression = impressions[1]

            val firstImpressionEvents = firstImpression.eventDataList

            // expecting exactly 2 samples given that we force lowest rendition
            // startup -> play
            assertThat(firstImpressionEvents).hasSize(2)
            val firstStartupSample = firstImpressionEvents.first()
            val firstPlayingSample = firstImpressionEvents[1]
            DataVerifier.verifyStartupSample(firstStartupSample)
            DataVerifier.verifyIsPlayingEvent(firstPlayingSample)
            DataVerifier.verifyStaticData(
                firstImpressionEvents,
                defaultSourceMetadata,
                TestSources.THEO_BIGBUCKBUNNY,
                TheoPlayerConstants.playerInfo,
                false,
            )
            DataVerifier.verifyInvariants(firstImpressionEvents)

            val secondImpressionEvents = secondImpression.eventDataList
            assertThat(secondImpressionEvents).hasSize(2)
            val secondStartupSample = secondImpressionEvents.first()
            val secondPlayingSample = secondImpressionEvents[1]
            DataVerifier.verifyStartupSample(secondStartupSample, false)
            DataVerifier.verifyIsPlayingEvent(secondPlayingSample)
            DataVerifier.verifyStaticData(
                secondImpressionEvents,
                redBullHlsSourceMetadata,
                TestSources.HLS_REDBULL,
                TheoPlayerConstants.playerInfo,
                false,
            )
            DataVerifier.verifyInvariants(secondImpressionEvents)
        }
    }

    @Test
    fun test_vod_audioLanguageTracking() {
        runBlockingTest {
            val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                collector.sourceMetadata = redBullHlsSourceMetadata
                collector.attachPlayer(player)
                player.source = hlsRedBullSourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                // switch source
                collector.detachPlayer()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)
            val events = impression.eventDataList

            // expecting exactly 2 samples given that we force lowest rendition
            // startup -> play
            assertThat(events).hasSize(2)
            val firstStartupSample = events.first()
            val firstPlayingSample = events[1]
            DataVerifier.verifyStartupSample(firstStartupSample)
            DataVerifier.verifyIsPlayingEvent(firstPlayingSample)
            DataVerifier.verifyStaticData(
                events,
                redBullHlsSourceMetadata,
                TestSources.HLS_REDBULL,
                TheoPlayerConstants.playerInfo,
                false,
            )
            DataVerifier.verifyInvariants(events)

            assertThat(firstPlayingSample.audioLanguage).isNotEmpty
            assertThat(firstPlayingSample.audioCodec).isNotEmpty
            assertThat(firstPlayingSample.audioBitrate).isNotZero
        }
    }

    @Test
    @Ignore("Events on subtitle changes are not implemented yet")
    fun test_vod_enableGermanSwitchToEnglishAndDisableSubtitles() {
        runBlockingTest {
            // arrange
            val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
            val sintelSource =
                TypedSource
                    .Builder(DASH_SINTEL_WITH_SUBTITLES.mpdUrl!!)
                    .type(SourceType.DASH)
                    .build()
            val sintelSourceDescription =
                SourceDescription
                    .Builder(sintelSource)
                    .build()

            // act
            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                collector.attachPlayer(player)
                player.source = sintelSourceDescription
                player.play()
            }

            // Wait for text tracks to be available
            Thread.sleep(1000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                // Enable German subtitle
                val germanTrack = player.textTracks.firstOrNull { it.language == "de" }
                germanTrack?.mode = TextTrackMode.SHOWING
                player.play()
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                // Switch to English subtitle
                val germanTrack = player.textTracks.firstOrNull { it.language == "de" }
                germanTrack?.mode = TextTrackMode.DISABLED
                val englishTrack = player.textTracks.firstOrNull { it.language == "en" }
                englishTrack?.mode = TextTrackMode.SHOWING
                player.play()
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                // Disable all subtitles
                val englishTrack = player.textTracks.firstOrNull { it.language == "en" }
                englishTrack?.mode = TextTrackMode.DISABLED
                player.play()
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 10000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.detachPlayer()
            }

            Thread.sleep(300)

            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
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
            assertThat(
                impression.eventDataList.size,
            ).isEqualTo(englishSubtitleSamples.size + germanSubtitleSamples.size + subtitleDisabledSamples.size)
        }
    }
}
