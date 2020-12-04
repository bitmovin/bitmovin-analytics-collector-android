package com.bitmovin.analytics.data.manipulators

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.EventData

/**
 * Decorates the event data with the m3u8 and mpd url if they are set in the bitmovin analytics configuration.
 */
open class ManifestUrlEventDataManipulator(
    private val bitmovinAnalyticsConfig: BitmovinAnalyticsConfig
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        if (bitmovinAnalyticsConfig.m3u8Url != null) {
            data.m3u8Url = bitmovinAnalyticsConfig.m3u8Url
        }
        if (bitmovinAnalyticsConfig.mpdUrl != null) {
            data.mpdUrl = bitmovinAnalyticsConfig.mpdUrl
        }
    }
}
