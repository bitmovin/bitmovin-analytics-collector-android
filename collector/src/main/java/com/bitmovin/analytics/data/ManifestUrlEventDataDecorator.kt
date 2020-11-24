package com.bitmovin.analytics.data

import com.bitmovin.analytics.BitmovinAnalyticsConfig

/**
 * Decorates the event data with the m3u8 and mpd url if they are set in the bitmovin analytics configuration.
 */
open class ManifestUrlEventDataDecorator(
    private val bitmovinAnalyticsConfig: BitmovinAnalyticsConfig
) : EventDataDecorator {
    override fun decorate(data: EventData) {
        if (bitmovinAnalyticsConfig.m3u8Url != null) {
            data.m3u8Url = bitmovinAnalyticsConfig.m3u8Url
        }
        if (bitmovinAnalyticsConfig.mpdUrl != null) {
            data.mpdUrl = bitmovinAnalyticsConfig.mpdUrl
        }
    }
}
