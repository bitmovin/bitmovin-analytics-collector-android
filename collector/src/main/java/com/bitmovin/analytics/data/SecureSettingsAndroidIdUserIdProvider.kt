package com.bitmovin.analytics.data

import android.content.Context
import android.provider.Settings

class SecureSettingsAndroidIdUserIdProvider(val context: Context) : UserIdProvider {
    private val userId = lazy { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }

    override fun userId(): String {
        return userId.value
    }
}
