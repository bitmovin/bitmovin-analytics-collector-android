package com.bitmovin.analytics.media3.api

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bitmovin.analytics.api.AnalyticsCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.media3.Media3PlayerCollector
import com.bitmovin.analytics.utils.Util

/**
 * Analytics Collector for the Media3-ExoPlayer
 */
@UnstableApi
interface IMedia3ExoPlayerCollector : AnalyticsCollector<ExoPlayer> {

    /**
     * The [sourceMetadata] which is used to enrich the analytics data with source specific
     * metadata.
     */
    var sourceMetadata: SourceMetadata

    /**
     * CustomData accessor to the current configured [sourceMetadata]
     *
     * Setting customData through this setter allows to reconfigure the customData during a session.
     * In case the player is in 'playing' or 'paused' state, an analytics event is triggered and a sample
     * is sent containing all measurements until the point in time of calling the method and the old customData.
     * All new samples will contain the new customData.
     *
     * More information can be found here:
     * https://developer.bitmovin.com/playback/docs/how-can-values-of-customdata-and-other-metadata-fields-be-changed
     */
    var customData: CustomData

    companion object Factory {

        /**
         * The version of the analytics collector
         *
         * For example "3.0.0"
         */
        @JvmStatic
        val sdkVersion = Util.analyticsVersion

        /**
         * Creates a collector instance configured via the provided [analyticsConfig], and the
         * optional [defaultMetadata].
         */
        @JvmStatic
        @JvmOverloads
        fun create(context: Context, analyticsConfig: AnalyticsConfig, defaultMetadata: DefaultMetadata = DefaultMetadata()): IMedia3ExoPlayerCollector {
            val collector = Media3PlayerCollector(analyticsConfig, context)
            collector.defaultMetadata = defaultMetadata
            return collector
        }
    }
}
