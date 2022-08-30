package com.bitmovin.analytics.data.manipulators

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.EventData

/**
 * Decorates the event data with the m3u8 and mpd url if they are set in the bitmovin analytics configuration.
 */
open class ManifestUrlEventDataManipulator(
    private val playerAdapter: PlayerAdapter,
    private val bitmovinAnalyticsConfig: BitmovinAnalyticsConfig
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        val currentSourceMetadata = playerAdapter.currentSourceMetadata
        if (currentSourceMetadata != null) {
            if (currentSourceMetadata.m3u8Url != null) {
                data.m3u8Url = currentSourceMetadata.m3u8Url
            }
            if (currentSourceMetadata.mpdUrl != null) {
                data.mpdUrl = currentSourceMetadata.mpdUrl
            }
            if (currentSourceMetadata.progUrl != null) {
                data.progUrl = currentSourceMetadata.progUrl
            }
        } else {
            if (bitmovinAnalyticsConfig.m3u8Url != null) {
                data.m3u8Url = bitmovinAnalyticsConfig.m3u8Url
            }
            if (bitmovinAnalyticsConfig.mpdUrl != null) {
                data.mpdUrl = bitmovinAnalyticsConfig.mpdUrl
            }
            if (bitmovinAnalyticsConfig.progUrl != null) {
                data.progUrl = bitmovinAnalyticsConfig.progUrl
            }
        }
    }
}
