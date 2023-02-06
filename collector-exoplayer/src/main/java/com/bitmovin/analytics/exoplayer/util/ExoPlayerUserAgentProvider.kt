package com.bitmovin.analytics.exoplayer.util

import android.content.Context
import com.bitmovin.analytics.utils.UserAgentProvider
import com.google.android.exoplayer2.util.Util

class ExoPlayerUserAgentProvider(context: Context) : UserAgentProvider {

    override val userAgent: String = generateUserAgent(context)

    private fun generateUserAgent(context: Context): String {
        val applicationName = getApplicationName(context)
        return Util.getUserAgent(context, applicationName)
    }

    private fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        var applicationName = "Unknown"
        if (applicationInfo != null) {
            val stringId = applicationInfo.labelRes
            if (stringId == 0 && applicationInfo.nonLocalizedLabel != null) {
                applicationName = applicationInfo.nonLocalizedLabel.toString()
            }
        }
        return applicationName
    }
}
