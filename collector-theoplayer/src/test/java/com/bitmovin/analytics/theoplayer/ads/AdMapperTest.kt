package com.bitmovin.analytics.theoplayer.ads

import com.theoplayer.android.api.ads.LinearAd
import com.theoplayer.android.api.ads.ima.GoogleImaAd
import com.theoplayer.android.api.event.ads.AdIntegrationKind
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import com.google.ads.interactivemedia.v3.api.Ad as ImaAd
import com.theoplayer.android.api.ads.Ad as TheoAd

/**
 * Covers how the different ad integrations report themselves on Android:
 *
 * | provider / ad system        | integration  | customIntegration |
 * |-----------------------------|--------------|-------------------|
 * | MediaKind CSB               | custom       | "mediakind"       |
 * | THEOads SGAI (deprecated)   | theoads      | -                 |
 * | OptiView Ads SGAI           | google-ima   | "optiview-ads"    |
 * | IMA pre-roll CSAI           | google-ima   | -                 |
 */
class AdMapperTest {
    @Test
    fun `MediaKind ad reports custom integration id as adModule and maps customData`() {
        val theoAd =
            linearAd(
                integration = AdIntegrationKind.CUSTOM,
                customIntegration = "mediakind",
                customData = CUSTOM_DATA,
            )

        val ad = AdMapper.fromTheoAd(theoAd)

        assertThat(ad.adModule).isEqualTo("mediakind")
        assertCustomDataIsMapped(ad)
        assertThat(ad.duration).isEqualTo(15000)
    }

    @Test
    fun `THEOads ad reports theoads as adModule`() {
        val theoAd = linearAd(integration = AdIntegrationKind.THEO_ADS, customIntegration = null)

        val ad = AdMapper.fromTheoAd(theoAd)

        assertThat(ad.adModule).isEqualTo("theoads")
        assertThat(ad.creativeId).isNull()
    }

    @Test
    fun `OptiView Ads ad reported as google-ima reports optiview-ads as adModule and maps customData over IMA data`() {
        val theoAd =
            googleImaAd(
                customIntegration = "optiview-ads",
                customData = CUSTOM_DATA,
            )

        val ad = AdMapper.fromTheoAd(theoAd)

        assertThat(ad.adModule).isEqualTo("optiview-ads")
        assertCustomDataIsMapped(ad)
        // IMA-only fields that are not part of customData are still mapped from the IMA ad
        assertThat(ad.bitrate).isEqualTo(IMA_BITRATE)
        assertThat(ad.duration).isEqualTo(15000)
    }

    @Test
    fun `OptiView Ads ad that is not a GoogleImaAd instance still maps customData`() {
        val theoAd =
            linearAd(
                integration = AdIntegrationKind.GOOGLE_IMA,
                customIntegration = "optiview-ads",
                customData = CUSTOM_DATA,
            )

        val ad = AdMapper.fromTheoAd(theoAd)

        assertThat(ad.adModule).isEqualTo("optiview-ads")
        assertCustomDataIsMapped(ad)
    }

    @Test
    fun `IMA pre-roll ad reports google-ima as adModule and maps IMA metadata`() {
        val theoAd = googleImaAd(customIntegration = null, customData = null)

        val ad = AdMapper.fromTheoAd(theoAd)

        assertThat(ad.adModule).isEqualTo("google-ima")
        assertThat(ad.creativeId).isEqualTo("ima-creative")
        assertThat(ad.creativeAdId).isEqualTo("ima-creative-ad")
        assertThat(ad.adSystemName).isEqualTo("IMA-System")
        assertThat(ad.title).isEqualTo("IMA Title")
        assertThat(ad.bitrate).isEqualTo(IMA_BITRATE)
    }

    private fun assertCustomDataIsMapped(ad: com.bitmovin.analytics.ads.Ad) {
        assertThat(ad.adSystemName).isEqualTo("custom-system")
        assertThat(ad.creativeId).isEqualTo("custom-creative")
        assertThat(ad.creativeAdId).isEqualTo("custom-creative-ad")
        assertThat(ad.advertiserName).isEqualTo("ACME Corp")
        assertThat(ad.title).isEqualTo("Custom Title")
        assertThat(ad.universalAdIdValue).isEqualTo("uaid-1")
        assertThat(ad.universalAdIdRegistry).isEqualTo("ad-id.org")
        assertThat(ad.isSlate).isFalse()
    }

    private fun linearAd(
        integration: AdIntegrationKind,
        customIntegration: String?,
        customData: Any? = null,
    ): TheoAd {
        val theoAd = mockk<LinearAd>(relaxed = true)
        every { theoAd.id } returns "ad-id"
        every { theoAd.type } returns "linear"
        every { theoAd.skipOffset } returns -1
        every { theoAd.integration } returns integration
        every { theoAd.customIntegration } returns customIntegration
        every { theoAd.customData } returns customData
        every { theoAd.durationAsDouble } returns 15.0
        return theoAd
    }

    private fun googleImaAd(
        customIntegration: String?,
        customData: Any?,
    ): TheoAd {
        val imaAd = mockk<ImaAd>(relaxed = true)
        every { imaAd.creativeAdId } returns "ima-creative-ad"
        every { imaAd.title } returns "IMA Title"
        every { imaAd.duration } returns 15.0

        val theoAd = mockkClass(GoogleImaAd::class, relaxed = true, moreInterfaces = arrayOf(LinearAd::class))
        every { theoAd.id } returns "ima-ad-id"
        every { theoAd.type } returns "linear"
        every { theoAd.skipOffset } returns -1
        every { theoAd.integration } returns AdIntegrationKind.GOOGLE_IMA
        every { theoAd.customIntegration } returns customIntegration
        every { theoAd.customData } returns customData
        every { theoAd.imaAd } returns imaAd
        every { theoAd.creativeId } returns "ima-creative"
        every { theoAd.adSystem } returns "IMA-System"
        every { theoAd.vastMediaBitrate } returns IMA_BITRATE
        every { theoAd.wrapperAdIds } returns emptyList()
        every { theoAd.universalAdIds } returns emptyList()
        every { (theoAd as LinearAd).durationAsDouble } returns 15.0
        return theoAd
    }

    companion object {
        private const val IMA_BITRATE = 1234

        private val CUSTOM_DATA =
            mapOf(
                "adSystem" to "custom-system",
                "creativeId" to "custom-creative",
                "creativeAdId" to "custom-creative-ad",
                "advertiserName" to "ACME Corp",
                "title" to "Custom Title",
                "universalAdIdValue" to "uaid-1",
                "universalAdIdRegistry" to "ad-id.org",
                "isSlate" to false,
            )
    }
}
