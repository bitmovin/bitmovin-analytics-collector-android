package com.bitmovin.analytics.data

import android.content.Context
import com.bitmovin.analytics.utils.Util

class SecureSettingsAndroidIdIdProvider(val context: Context) : UserIdProvider {
    override fun userId(): String {
        return Util.getUserId(context)
    }
}
