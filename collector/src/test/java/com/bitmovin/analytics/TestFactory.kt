package com.bitmovin.analytics

import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.UserIdProvider
import com.bitmovin.analytics.utils.UserAgentProvider
import io.mockk.mockk

object TestFactory {
    fun createEventDataFactory(
        config: BitmovinAnalyticsConfig,
        userIdProvider: UserIdProvider? = null,
        userAgentProvider: UserAgentProvider? = null,
    ): EventDataFactory {
        return EventDataFactory(
            config,
            userIdProvider ?: mockk(relaxed = true),
            userAgentProvider ?: mockk(relaxed = true),
        )
    }
}
