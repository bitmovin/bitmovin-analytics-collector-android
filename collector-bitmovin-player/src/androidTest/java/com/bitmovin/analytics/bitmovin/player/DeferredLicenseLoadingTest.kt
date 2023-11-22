package com.bitmovin.analytics.bitmovin.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val DEFERRED_LICENSE_KEY_PLACEHOLDER = "DEFERRED"
private const val PLAYER_LICENSE_KEY = "a6e31908-550a-4f75-b4bc-a9d89880a733"
private const val ANALYTICS_LICENSE_KEY = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0"

@RunWith(AndroidJUnit4::class)
class DeferredLicenseLoadingTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    private val defaultSource = Source.create(
        SourceConfig(
            TestSources.HLS_REDBULL.m3u8Url!!,
            SourceType.Hls,
        ),
    )
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig

    @Before
    fun setup() {
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)

        val mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(
            analyticsKey = DEFERRED_LICENSE_KEY_PLACEHOLDER,
            backendUrl = mockedIngressUrl,
        )
    }

    @After
    fun tearDown() {
        mainScope.launch { defaultPlayer.destroy() }
        // wait a bit for player to be destroyed
        Thread.sleep(100)
    }

    @Test
    fun test_deferredLoadedLicenseKey_isUsedInEventData_whenUsingDeferredPlaceholder() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)
        initializePlayer()
        lateinit var deferredLicenseKey: String

        // act
        mainScope.launch {
            defaultPlayer.on<PlayerEvent.LicenseValidated> {
                deferredLicenseKey = it.data.analytics.key!!
            }
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(defaultSource)
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            collector.detachPlayer()
            defaultPlayer.destroy()
        }

        // assert
        val impressionList = MockedIngress.extractImpressions()
        val eventDataList = impressionList.single().eventDataList
        assertThat(eventDataList.map { it.key }.distinct()).isEqualTo(listOf(deferredLicenseKey))
    }

    @Test
    fun test_deferredLoadedLicenseKey_isUsedInErrorDetail_whenUsingDeferredPlaceholder() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)
        initializePlayer()
        val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
        val nonExistingSource = Source.create(
            SourceConfig.fromUrl(nonExistingStreamSample.uri.toString()),
        )
        lateinit var deferredLicenseKey: String

        // act
        mainScope.launch {
            defaultPlayer.on<PlayerEvent.LicenseValidated> {
                deferredLicenseKey = it.data.analytics.key!!
            }
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(nonExistingSource)
        }

        // it seems to take a while until the error is consistently reported
        Thread.sleep(10000)

        mainScope.launch { collector.detachPlayer() }

        Thread.sleep(100)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        val errorDetailList = impressionList.single().errorDetailList
        assertThat(errorDetailList.map { it.licenseKey }.distinct())
            .isEqualTo(listOf(deferredLicenseKey))
    }

    @Test
    fun test_deferredLoadedLicenseKey_isUsedInAdEventData_whenUsingDeferredPlaceholder() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)
        initializePlayer(
            advertisingConfig = AdvertisingConfig(
                AdItem(
                    "pre",
                    AdSource(
                        AdSourceType.Progressive,
                        "https://bitmovin-a.akamaihd.net/content/testing/ads/testad2s.mp4",
                    ),
                ),
            ),
        )
        lateinit var deferredLicenseKey: String

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.on<PlayerEvent.LicenseValidated> {
                deferredLicenseKey = it.data.analytics.key!!
            }
            defaultPlayer.load(defaultSource)
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            collector.detachPlayer()
            defaultPlayer.destroy()
        }

        // assert
        val impressionList = MockedIngress.extractImpressions()
        val adEventDataList = impressionList.single().adEventDataList
        assertThat(adEventDataList.map { it.key }.distinct()).isEqualTo(listOf(deferredLicenseKey))
    }

    @Test
    fun test_deferredLoadedLicenseKey_isNotUsed_whenNotUsingDeferredPlaceholder() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(
            appContext,
            defaultAnalyticsConfig.copy(licenseKey = ANALYTICS_LICENSE_KEY),
        )
        initializePlayer()
        var deferredLicenseKey: String? = null

        // act
        mainScope.launch {
            defaultPlayer.on<PlayerEvent.LicenseValidated> {
                deferredLicenseKey = it.data.analytics.key
            }
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(defaultSource)
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.launch { collector.detachPlayer() }

        // assert
        val impressionList = MockedIngress.extractImpressions()
        val eventDataList = impressionList.single().eventDataList
        assertThat(eventDataList.map { it.key }.distinct()).isEqualTo(listOf(ANALYTICS_LICENSE_KEY))
        assertThat(deferredLicenseKey).isNotNull
    }

    @Test
    fun test_doesNotAuthenticatesTheCollector_whenAttachedAfterLicenseValidateEventAndUsingDeferredLicenseKeyPlaceholder() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)
        initializePlayer()
        var deferredLicenseKey: String? = null

        // act
        mainScope.launch {
            defaultPlayer.on<PlayerEvent.LicenseValidated> {
                deferredLicenseKey = it.data.analytics.key!!
            }
        }

        PlaybackUtils.waitUntil { deferredLicenseKey != null }

        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(defaultSource)
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            collector.detachPlayer()
            defaultPlayer.destroy()
        }

        // assert
        assertThat(MockedIngress.extractImpressions()).isEmpty()
    }

    private fun initializePlayer(
        playbackConfig: PlaybackConfig = PlaybackConfig(isAutoplayEnabled = true),
        advertisingConfig: AdvertisingConfig = AdvertisingConfig(),
    ) {
        defaultPlayer = Player.create(
            appContext,
            PlayerConfig(
                key = PLAYER_LICENSE_KEY,
                playbackConfig = playbackConfig,
                advertisingConfig = advertisingConfig,
            ),
        )
    }
}
