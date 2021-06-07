package com.bitmovin.analytics.data

import com.bitmovin.analytics.utils.Util

class RandomisedUserIdIdProvider : UserIdProvider {
    private val userId: String = Util.getRandomUserId()

    override fun userId(): String {
        return userId
    }
}
