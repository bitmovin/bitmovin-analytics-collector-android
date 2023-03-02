package com.bitmovin.analytics

import com.bitmovin.analytics.data.CustomData

/**
 * Public interface which is shared by all collectors
 */
interface AnalyticsCollector<TPlayer> {

    /**
     * Setting customData through this setter allows to reconfigure the customData during a session.
     * In case the player is in 'playing' or 'paused' state, an analytics event is triggered and a sample
     * is sent containing all measurements until the point in time of calling the method and the old customData.
     * All new samples will contain the new customData.
     *
     * More information can be found here:
     * https://developer.bitmovin.com/playback/docs/how-can-values-of-customdata-and-other-metadata-fields-be-changed
     */
    var customData: CustomData

    /**
     * The impressionId of the current session
     */
    val impressionId: String?

    /**
     * The currently active analytics configuration
     */
    val config: BitmovinAnalyticsConfig

    /**
     * The version of the analytics collector
     */
    val version: String

    /**
     * The userId that is sent with each analytics sample.
     * (ANDROID_ID or a random String, depending on the analytics configuration)
     */
    val userId: String

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
     * setCustomDataOnce sends a sample with state='customdatachanged' containing the [customData].
     * It does not change the permanently configured customData that is set through the config or setCustomData.
     *
     * More information can be found here:
     * https://developer.bitmovin.com/playback/docs/how-can-values-of-customdata-and-other-metadata-fields-be-changed
     */
    fun setCustomDataOnce(customData: CustomData)
}
