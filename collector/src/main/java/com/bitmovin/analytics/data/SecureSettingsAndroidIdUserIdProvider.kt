package com.bitmovin.analytics.data

import android.content.Context
import android.provider.Settings

class SecureSettingsAndroidIdUserIdProvider(val context: Context) : UserIdProvider {
    override fun userId(): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}
