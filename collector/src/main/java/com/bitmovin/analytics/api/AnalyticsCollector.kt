package com.bitmovin.analytics.api

// TODO: add missing descriptions

/**
 * Public interface which is shared by all collectors
 */
interface AnalyticsCollector<TPlayer> {

    /**
     * The active configuration of the analytics collector.
     */
    val config: AnalyticsConfig

    /**
     * The impressionId of the current session
     */
    val impressionId: String?

    /**
     * The version of the analytics collector
     */
    val version: String

    /**
     * The userId that is sent with each analytics sample.
     * (ANDROID_ID or a random String, depending on the analytics configuration)
     */
    val userId: String

    // TODO: discuss with player folks when to use properties and when to use explicit getter/setter methods
    val customData: CustomData

    var defaultMetadata: DefaultMetadata

    fun setCurrentSourceMetadata(sourceMetadata: SourceMetadata)
    fun getCurrentSourceMetadata(): SourceMetadata

    fun setDefaultCustomData(customData: CustomData)
    fun getDefaultCustomData(): CustomData?
    fun setCurrentSourceCustomData(customData: CustomData)
    fun getCurrentSourceCustomData(): CustomData?

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
     * It does not change the permanently configured customData that is set through metadata or setCustomData.
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
     * It does not change the permanently configured customData that is set through metadata or setCustomData.
     *
     * More information can be found here:
     * https://developer.bitmovin.com/playback/docs/how-can-values-of-customdata-and-other-metadata-fields-be-changed
     */
    fun sendCustomDataEvent(customData: CustomData)
}
