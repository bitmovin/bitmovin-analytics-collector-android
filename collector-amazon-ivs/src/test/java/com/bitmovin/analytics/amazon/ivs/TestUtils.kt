package com.bitmovin.analytics.amazon.ivs

import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.enums.PlayerType

class TestUtils {
    companion object {
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
                deviceInfo, playerInfo, "aaa", "bbb", null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, "testUserAgent",
            )
        }
    }
}
