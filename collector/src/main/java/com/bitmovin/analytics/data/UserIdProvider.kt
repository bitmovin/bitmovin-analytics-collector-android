package com.bitmovin.analytics.data

import android.content.Context
import com.bitmovin.analytics.utils.Util

class UserIdProvider(val context: Context) {
    fun userId(): String {
        return Util.getUserId(context)
    }
}
