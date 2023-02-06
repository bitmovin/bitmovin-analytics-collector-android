package com.bitmovin.analytics.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build

class PlayerUserAgentProvider(context: Context, playerAgent: String) : UserAgentProvider {

    override val userAgent = generateUserAgent(context, playerAgent)

    private fun generateUserAgent(context: Context, playerAgent: String): String {
        val applicationName = getApplicationName(context)
        val versionName: String = getVersionName(context)
        return buildUserAgent(applicationName, versionName, playerAgent)
    }

    private fun buildUserAgent(
        applicationName: String?,
        versionName: String,
        playerAgent: String,
    ) = "$applicationName/$versionName (Linux;Android ${Build.VERSION.RELEASE}) $playerAgent"

    private fun getApplicationName(context: Context): String {
        val applicationInfo: ApplicationInfo? = context.applicationInfo
        val stringId = applicationInfo?.labelRes
        val defaultValue = "Unknown"
        if (stringId != 0) {
            return defaultValue
        }
        return applicationInfo.nonLocalizedLabel.toString()
    }

    private fun getVersionName(context: Context): String {
        var versionName = "?"
        try {
            val packageName = context.packageName
            val info = context.packageManager?.getPackageInfo(packageName, 0)
            versionName = info?.versionName ?: versionName
        } catch (_: Exception) {
        }
        return versionName
    }
}
