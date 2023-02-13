package com.bitmovin.analytics.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build

/**
 *
 * This class returns value from systemUserAgent if provided, otherwise
 * generates userAgent string from applicationInfo and packageInfo
 *
 * @property ApplicationInfo is application info obtained from context
 * @property PackageInfo is package info obtained from context
 * @property String is user agent obtained from `System.getProperty(httpAgent)`
 */
class UserAgentProvider(applicationInfo: ApplicationInfo?, packageInfo: PackageInfo?, systemUserAgent: String?) {

    val userAgent: String = systemUserAgent ?: generateUserAgent(applicationInfo, packageInfo)

    private fun generateUserAgent(applicationInfo: ApplicationInfo?, packageInfo: PackageInfo?): String {
        val applicationName = getApplicationName(applicationInfo)
        val versionName: String = getVersionName(packageInfo)
        return buildUserAgent(applicationName, versionName)
    }

    private fun buildUserAgent(
        applicationName: String?,
        versionName: String,
    ) = "$applicationName/$versionName (Linux;Android ${Build.VERSION.RELEASE})"

    private fun getApplicationName(applicationInfo: ApplicationInfo?): String {
        val stringId = applicationInfo?.labelRes
        val defaultValue = "Unknown"
        if (stringId != 0) {
            return defaultValue
        }
        return applicationInfo.nonLocalizedLabel.toString()
    }

    private fun getVersionName(info: PackageInfo?): String {
        var versionName = "?"
        versionName = info?.versionName ?: versionName

        return versionName
    }
}
