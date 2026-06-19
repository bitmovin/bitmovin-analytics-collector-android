@file:Suppress("DEPRECATION")

package com.bitmovin.analytics.api.ssai

import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.ads.AdBreakMetadata
import com.bitmovin.analytics.api.ads.AdMetadata
import com.bitmovin.analytics.ssai.toAdBreakMetadata
import com.bitmovin.analytics.ssai.toAdMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration

class SsaiMetadataMappersTest {
    @Test
    fun `toAdMetadata carries over all fields of the deprecated SsaiAdMetadata`() {
        val customData = CustomData(customData1 = "cd1")
        val deprecated =
            SsaiAdMetadata(
                adId = "ad-id",
                adSystem = "ad-system",
                customData = customData,
                isSlate = true,
                duration = Duration.ofSeconds(15),
            )

        val mapped = deprecated.toAdMetadata()

        assertThat(mapped.adId).isEqualTo("ad-id")
        assertThat(mapped.adSystem).isEqualTo("ad-system")
        assertThat(mapped.customData).isEqualTo(customData)
        assertThat(mapped.isSlate).isTrue()
        assertThat(mapped.duration).isEqualTo(Duration.ofSeconds(15))
    }

    @Test
    fun `toAdMetadata keeps default values for an empty SsaiAdMetadata`() {
        val mapped = SsaiAdMetadata().toAdMetadata()

        assertThat(mapped.adId).isNull()
        assertThat(mapped.adSystem).isNull()
        assertThat(mapped.customData).isNull()
        assertThat(mapped.isSlate).isFalse()
        assertThat(mapped.duration).isNull()
    }

    @Test
    fun `toAdBreakMetadata carries over all fields of the deprecated SsaiAdBreakMetadata`() {
        val deprecated =
            SsaiAdBreakMetadata(
                adPosition = SsaiAdPosition.MIDROLL,
                expectedPaidAds = 3,
                expectedSlates = 1,
            )

        val mapped = deprecated.toAdBreakMetadata()

        assertThat(mapped.adPosition).isEqualTo(SsaiAdPosition.MIDROLL)
        assertThat(mapped.expectedPaidAds).isEqualTo(3)
        assertThat(mapped.expectedSlates).isEqualTo(1)
    }

    @Test
    fun `toAdBreakMetadata keeps default values for an empty SsaiAdBreakMetadata`() {
        val mapped = SsaiAdBreakMetadata().toAdBreakMetadata()

        assertThat(mapped.adPosition).isNull()
        assertThat(mapped.expectedPaidAds).isNull()
        assertThat(mapped.expectedSlates).isNull()
    }

    @Test
    fun `AdMetadata Builder produces equal instances for equal input`() {
        val a = AdMetadata.Builder().setAdId("id").setAdSystem("sys").build()
        val b = AdMetadata.Builder().setAdId("id").setAdSystem("sys").build()

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `AdBreakMetadata Builder produces equal instances for equal input`() {
        val a = AdBreakMetadata.Builder().setAdPosition(SsaiAdPosition.PREROLL).setExpectedPaidAds(2).build()
        val b = AdBreakMetadata.Builder().setAdPosition(SsaiAdPosition.PREROLL).setExpectedPaidAds(2).build()

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }
}
