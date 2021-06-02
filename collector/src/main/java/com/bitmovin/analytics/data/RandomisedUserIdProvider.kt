package com.bitmovin.analytics.data

import com.bitmovin.analytics.utils.Util

class RandomisedUserIdProvider : UserProvider {
    private val userId: String = Util.getRandomUserId()

    override fun userId(): String {
        return userId
    }
}
