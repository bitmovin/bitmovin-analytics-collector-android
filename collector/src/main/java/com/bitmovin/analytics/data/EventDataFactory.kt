package com.bitmovin.analytics.data

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig

open class EventDataFactory(
    val config: BitmovinAnalyticsConfig,
    val context: Context,
    private val deviceInformationProvider: DeviceInformationProvider,
    private val userIdProvider: UserIdProvider
) {
    fun build(impressionId: String): EventData {
        return EventData(config, impressionId, deviceInformationProvider.getDeviceInformation(), userIdProvider.userId())
    }
}
