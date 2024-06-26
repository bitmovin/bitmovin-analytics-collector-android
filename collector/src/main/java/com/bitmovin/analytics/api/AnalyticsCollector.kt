package com.bitmovin.analytics.api

import com.bitmovin.analytics.api.ssai.SsaiApi

/**
 * Public interface which is shared by all collectors
 */
interface AnalyticsCollector<TPlayer> {
    /**
     * The impressionId of the current session
     */
    val impressionId: String?

    /**
     * The userId that is sent with each analytics sample.
     * (ANDROID_ID or a random String, depending on the analytics configuration)
     */
    val userId: String

    var defaultMetadata: DefaultMetadata

    /**
     * Attaches player to the analytics collector and starts listening to player events
     */
    fun attachPlayer(player: TPlayer)

    /**
     * Detaches player from the analytics collector.
     *
     * This should always be called before releasing/destroying the player and before loading a new
     * source for the player, followed by attaching the collector again.
     */
    fun detachPlayer()

    /**
     * Track Server side ad insertion (ssai) related data via the [ssai] Api.
     */
    val ssai: SsaiApi

    /**
     * setCustomDataOnce sends a sample with state='customdatachanged' containing the [customData].
     * It does not change the configured customData that is set through defaultMetadata or sourceMetadata..
     *
     * More information can be found here:
     * https://developer.bitmovin.com/playback/docs/how-can-values-of-customdata-and-other-metadata-fields-be-changed
     */
    @Deprecated(
        """Please use {@link #sendCustomDataEvent(CustomData)} instead.""",
        ReplaceWith("sendCustomDataEvent(customData)"),
    )
    fun setCustomDataOnce(customData: CustomData)

    /**
     * sendCustomDataEvent sends a sample with state='customdatachanged' containing the [customData].
     * It does not change the configured customData that is set through defaultMetadata or sourceMetadata.
     *
     * More information can be found here:
     * https://developer.bitmovin.com/playback/docs/how-can-values-of-customdata-and-other-metadata-fields-be-changed
     */
    fun sendCustomDataEvent(customData: CustomData)
}
