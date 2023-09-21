package com.bitmovin.analytics.amazon.ivs

import android.content.Context
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.enums.PlayerType
import io.mockk.mockk

object TestUtils {
    fun createMinimalEventData(): EventData {
        val deviceInfo = DeviceInformation(
            "manufactorer",
            "model",
            false,
            "locale",
            "domain",
            0,
            0,
            null,
            null,
            null,
            null,
        )
        val playerInfo = PlayerInfo("playerTech", PlayerType.AMAZON_IVS)

        return EventData(
            deviceInfo, playerInfo, CustomData(), "aaa", "bbb",
            null, null, null, null, null,
            null, "testUserAgent",
        )
    }

    // helper to create a context mock for the java tests
    fun createMockContext(): Context {
        return mockk(relaxed = true)
    }
}
