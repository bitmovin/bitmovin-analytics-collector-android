package com.bitmovin.analytics.ssai

import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdPosition
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.analytics.api.ssai.SsaiAdQuartileMetadata
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.utils.SystemTimeService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SsaiEngagementMetricsServiceTest {
    private lateinit var ssaiEngagementMetricsService: SsaiEngagementMetricsService

    private lateinit var ssaiEngagementMetricsServiceDisabled: SsaiEngagementMetricsService
    private val analytics: BitmovinAnalytics = mockk()
    private val playerAdapter: PlayerAdapter = mockk()
    private val handlerMock = mockk<Handler>()
    private val systemTimeServiceMock = mockk<SystemTimeService>()
    private val analyticsConfig =
        AnalyticsConfig(
            "dummyLicenseKey",
            ssaiEngagementTrackingEnabled = true,
        )

    @Before
    fun setUp() {
        // dummy default mock setup to run tests
        every { systemTimeServiceMock.elapsedRealtime() }.returns(12L)
        ssaiEngagementMetricsService =
            SsaiEngagementMetricsService(
                analytics,
                analyticsConfig, playerAdapter, handlerMock, systemTimeServiceMock,
            )

        ssaiEngagementMetricsServiceDisabled =
            SsaiEngagementMetricsService(
                analytics,
                AnalyticsConfig("dummyLicense"), playerAdapter, handlerMock, systemTimeServiceMock,
            )
    }

    @Test
    fun `markAdStart should createSampleWithStartedFlag`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)
        ssaiEngagementMetricsService.flushCurrentAdSample()

        val adEventDataSlot = slot<AdEventData>()
        verify(exactly = 1) { analytics.sendAdEventData(capture(adEventDataSlot)) }
        val adEventData = adEventDataSlot.captured
        assertThat(adEventData.adType).isEqualTo(AdType.SERVER_SIDE.value)
        assertThat(adEventData.adImpressionId).isNotEmpty()
        assertThat(adEventData.started).isEqualTo(1)
        assertThat(adEventData.quartile1).isEqualTo(0)
        assertThat(adEventData.midpoint).isEqualTo(0)
        assertThat(adEventData.quartile3).isEqualTo(0)
        assertThat(adEventData.completed).isEqualTo(0)
        assertThat(adEventData.adId).isEqualTo("testId")
        assertThat(adEventData.adSystem).isEqualTo("testAdSystem")
        assertThat(adEventData.adIndex).isEqualTo(0)
        assertThat(adEventData.adPosition).isEqualTo("midroll")
    }

    @Test
    fun `markAdStart should flush out existing sample before creating new one`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 1)

        verify(exactly = 1) { analytics.sendAdEventData(any()) }
    }

    @Test
    fun `markAdStart should be noop if adEngagementTracking is disabled`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsServiceDisabled.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)
        verify(exactly = 0) { analytics.sendAdEventData(any()) }
    }

    @Test
    fun `markAdStart should create new AdId on every call`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 1)
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 2)

        ssaiEngagementMetricsService.flushCurrentAdSample()

        val adEventDataList = mutableListOf<AdEventData>()
        verify(exactly = 3) { analytics.sendAdEventData(capture(adEventDataList)) }
        val firstAd = adEventDataList[0]
        val secondAd = adEventDataList[1]
        val thirdAd = adEventDataList[2]
        assertThat(firstAd.adImpressionId).isNotEmpty()
        assertThat(secondAd.adImpressionId).isNotEmpty()
        assertThat(thirdAd.adImpressionId).isNotEmpty()
        assertThat(firstAd.adImpressionId).isNotEqualTo(secondAd.adImpressionId)
        assertThat(secondAd.adImpressionId).isNotEqualTo(thirdAd.adImpressionId)
    }

    @Test
    fun `markQuartileFinished should mark correct quartile field and set data`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        val adQuartileMetadata = SsaiAdQuartileMetadata(failedBeaconUrl = "dummyUrl")

        ssaiEngagementMetricsService.markQuartileFinished(
            SsaiAdPosition.MIDROLL,
            SsaiAdQuartile.FIRST,
            adMetadata,
            adQuartileMetadata,
            1,
        )
        ssaiEngagementMetricsService.flushCurrentAdSample()

        val adEventDataSlot = slot<AdEventData>()
        verify(exactly = 1) { analytics.sendAdEventData(capture(adEventDataSlot)) }
        val adEventData = adEventDataSlot.captured
        assertThat(adEventData.adType).isEqualTo(AdType.SERVER_SIDE.value)
        assertThat(adEventData.adImpressionId).isNotEmpty()
        assertThat(adEventData.started).isEqualTo(0)
        assertThat(adEventData.quartile1).isEqualTo(1)
        assertThat(adEventData.midpoint).isEqualTo(0)
        assertThat(adEventData.quartile3).isEqualTo(0)
        assertThat(adEventData.completed).isEqualTo(0)
        assertThat(adEventData.quartile1FailedBeaconUrl).isEqualTo("dummyUrl")
        assertThat(adEventData.adId).isEqualTo("testId")
        assertThat(adEventData.adSystem).isEqualTo("testAdSystem")
        assertThat(adEventData.adIndex).isEqualTo(1)
        assertThat(adEventData.adPosition).isEqualTo("midroll")
    }

    @Test
    fun `markQuartileFinished should NOT create sample if adEngagementTracking is disabled`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        val adQuartileMetadata = SsaiAdQuartileMetadata(failedBeaconUrl = "dummyUrl")

        ssaiEngagementMetricsServiceDisabled.markQuartileFinished(
            SsaiAdPosition.MIDROLL,
            SsaiAdQuartile.FIRST,
            adMetadata,
            adQuartileMetadata,
            1,
        )
        ssaiEngagementMetricsServiceDisabled.flushCurrentAdSample()

        verify(exactly = 0) { analytics.sendAdEventData(any()) }
    }

    @Test
    fun `markQuartileFinished should merge quartile fields field and set data`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        val adQuartileMetadata = SsaiAdQuartileMetadata(failedBeaconUrl = "dummyUrl")

        ssaiEngagementMetricsService.markQuartileFinished(
            SsaiAdPosition.MIDROLL,
            SsaiAdQuartile.FIRST,
            adMetadata,
            adQuartileMetadata,
            1,
        )
        ssaiEngagementMetricsService.markQuartileFinished(
            SsaiAdPosition.MIDROLL,
            SsaiAdQuartile.MIDPOINT,
            adMetadata,
            adQuartileMetadata,
            1,
        )
        ssaiEngagementMetricsService.markQuartileFinished(
            SsaiAdPosition.MIDROLL,
            SsaiAdQuartile.THIRD,
            adMetadata,
            adQuartileMetadata,
            1,
        )
        ssaiEngagementMetricsService.markQuartileFinished(
            SsaiAdPosition.MIDROLL,
            SsaiAdQuartile.COMPLETED,
            adMetadata,
            adQuartileMetadata,
            1,
        )
        ssaiEngagementMetricsService.flushCurrentAdSample()

        val adEventDataSlot = slot<AdEventData>()
        verify(exactly = 1) { analytics.sendAdEventData(capture(adEventDataSlot)) }
        val adEventData = adEventDataSlot.captured
        assertThat(adEventData.adType).isEqualTo(AdType.SERVER_SIDE.value)
        assertThat(adEventData.adImpressionId).isNotEmpty()
        assertThat(adEventData.quartile1).isEqualTo(1)
        assertThat(adEventData.midpoint).isEqualTo(1)
        assertThat(adEventData.quartile3).isEqualTo(1)
        assertThat(adEventData.completed).isEqualTo(1)
        assertThat(adEventData.quartile1FailedBeaconUrl).isEqualTo("dummyUrl")
        assertThat(adEventData.midpointFailedBeaconUrl).isEqualTo("dummyUrl")
        assertThat(adEventData.quartile3FailedBeaconUrl).isEqualTo("dummyUrl")
        assertThat(adEventData.completedFailedBeaconUrl).isEqualTo("dummyUrl")
        assertThat(adEventData.adId).isEqualTo("testId")
        assertThat(adEventData.adSystem).isEqualTo("testAdSystem")
        assertThat(adEventData.adIndex).isEqualTo(1)
        assertThat(adEventData.adPosition).isEqualTo("midroll")
    }

    @Test
    fun `markQuartileFinished should be noop if called twice for same ad`() {
        ssaiEngagementMetricsService.markQuartileFinished(SsaiAdPosition.MIDROLL, SsaiAdQuartile.FIRST, null, null, 1)
        ssaiEngagementMetricsService.flushCurrentAdSample()

        ssaiEngagementMetricsService.markQuartileFinished(SsaiAdPosition.MIDROLL, SsaiAdQuartile.FIRST, null, null, 1)
        ssaiEngagementMetricsService.flushCurrentAdSample()
        verify(exactly = 1) { analytics.sendAdEventData(any()) }
    }

    @Test
    fun `flushCurrentAdSample sends out existing sample and clears it afterwards`() {
        ssaiEngagementMetricsService.markAdStart(null, null, 0)

        // first call should send out sample
        ssaiEngagementMetricsService.flushCurrentAdSample()
        // second call should be noop
        ssaiEngagementMetricsService.flushCurrentAdSample()

        verify(exactly = 1) { analytics.sendAdEventData(any()) }
    }

    @Test
    fun `flushCurrentAdSample is noop if no sample is existing`() {
        ssaiEngagementMetricsService.flushCurrentAdSample()
        verify(exactly = 0) { analytics.sendAdEventData(any()) }
    }

    @Test
    fun `sendAdErrorSample should sendout existing sample with error data`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)
        ssaiEngagementMetricsService.markQuartileFinished(SsaiAdPosition.MIDROLL, SsaiAdQuartile.FIRST, adMetadata, null, 0)
        ssaiEngagementMetricsService.sendAdErrorSample(SsaiAdPosition.MIDROLL, adMetadata, 0, 1234, "testMessage")

        val adEventDataSlot = slot<AdEventData>()
        verify(exactly = 1) { analytics.sendAdEventData(capture(adEventDataSlot)) }
        val adEventData = adEventDataSlot.captured
        assertThat(adEventData.adType).isEqualTo(AdType.SERVER_SIDE.value)
        assertThat(adEventData.adImpressionId).isNotEmpty()
        assertThat(adEventData.started).isEqualTo(1)
        assertThat(adEventData.quartile1).isEqualTo(1)
        assertThat(adEventData.midpoint).isEqualTo(0)
        assertThat(adEventData.quartile3).isEqualTo(0)
        assertThat(adEventData.completed).isEqualTo(0)
        assertThat(adEventData.quartile1FailedBeaconUrl).isNull()
        assertThat(adEventData.midpointFailedBeaconUrl).isNull()
        assertThat(adEventData.quartile3FailedBeaconUrl).isNull()
        assertThat(adEventData.completedFailedBeaconUrl).isNull()
        assertThat(adEventData.adId).isEqualTo("testId")
        assertThat(adEventData.adSystem).isEqualTo("testAdSystem")
        assertThat(adEventData.adIndex).isEqualTo(0)
        assertThat(adEventData.adPosition).isEqualTo("midroll")
        assertThat(adEventData.errorCode).isEqualTo(1234)
        assertThat(adEventData.errorMessage).isEqualTo("testMessage")
    }

    @Test
    fun `sendAdErrorSample should NOT sendout sample if ssaiEngagement is disabled`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsServiceDisabled.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)
        ssaiEngagementMetricsServiceDisabled.sendAdErrorSample(SsaiAdPosition.MIDROLL, adMetadata, 0, 1234, "testMessage")

        verify(exactly = 0) { analytics.sendAdEventData(any()) }
    }

    @Test
    fun `sendAdErrorSample is not send twice on one ad`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)
        ssaiEngagementMetricsService.sendAdErrorSample(SsaiAdPosition.MIDROLL, adMetadata, 0, 1234, "testMessage")
        ssaiEngagementMetricsService.sendAdErrorSample(SsaiAdPosition.MIDROLL, adMetadata, 0, 1234, "testMessage")
        verify(exactly = 1) { analytics.sendAdEventData(any()) }
    }

    @Test
    fun `sendAdErrorSample is called once per ad`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)
        ssaiEngagementMetricsService.sendAdErrorSample(SsaiAdPosition.MIDROLL, adMetadata, 0, 1234, "testMessage")

        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 1)
        ssaiEngagementMetricsService.sendAdErrorSample(SsaiAdPosition.MIDROLL, adMetadata, 0, 1234, "testMessage")
        verify(exactly = 2) { analytics.sendAdEventData(any()) }
    }

    @Test
    fun `timeout for flushing is reset on ad started`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)

        verify(exactly = 2) { handlerMock.removeCallbacksAndMessages(any()) }
        verify(exactly = 1) { handlerMock.postDelayed(any(), any()) }
    }

    @Test
    fun `timeout for flushing is postponed on quartile call`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)

        // we clear the mock to only test that calls happen due to the quartile call, and not the started call
        clearMocks(handlerMock)

        ssaiEngagementMetricsService.markQuartileFinished(SsaiAdPosition.MIDROLL, SsaiAdQuartile.FIRST, adMetadata, null, 0)

        verify(exactly = 1) { handlerMock.removeCallbacksAndMessages(any()) }
        verify(exactly = 1) { handlerMock.postDelayed(any(), any()) }
    }

    @Test
    fun `timeout for flushing is cleared on error`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)

        // we clear the mock to only test that calls happen due to the error, and not the started call
        clearMocks(handlerMock)

        ssaiEngagementMetricsService.sendAdErrorSample(SsaiAdPosition.MIDROLL, adMetadata, 0, 123, "testError")
        verify(exactly = 1) { handlerMock.removeCallbacksAndMessages(any()) }
        verify(exactly = 0) { handlerMock.postDelayed(any(), any()) }
    }

    @Test
    fun `timeout for flushing is cleared on completed`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)

        // we clear the mock to only test that calls happen due to the completed call, and not the started call
        clearMocks(handlerMock)
        ssaiEngagementMetricsService.markQuartileFinished(SsaiAdPosition.MIDROLL, SsaiAdQuartile.COMPLETED, adMetadata, null, 0)
        verify(exactly = 1) { handlerMock.removeCallbacksAndMessages(any()) }
        verify(exactly = 0) { handlerMock.postDelayed(any(), any()) }
    }

    @Test
    fun `timeout for flushing is cleared on flushOfCurrentSamples`() {
        val adMetadata = SsaiAdMetadata(adId = "testId", adSystem = "testAdSystem")
        ssaiEngagementMetricsService.markAdStart(SsaiAdPosition.MIDROLL, adMetadata, 0)

        // we clear the mock to only test that calls happen due to the adBreakEnd call, and not the started call
        clearMocks(handlerMock)
        ssaiEngagementMetricsService.flushCurrentAdSample()
        verify(exactly = 1) { handlerMock.removeCallbacksAndMessages(any()) }
        verify(exactly = 0) { handlerMock.postDelayed(any(), any()) }
    }
}
