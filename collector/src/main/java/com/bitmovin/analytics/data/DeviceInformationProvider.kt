package com.bitmovin.analytics.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.bitmovin.analytics.utils.Util

open class DeviceInformationProvider(val context: Context, val userAgent: String) {
    fun getDeviceInformation(): DeviceInformation {
        return DeviceInformation(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                locale = Util.getLocale(),
                packageName = context.packageName,
                screenWidth = context?.getResources()?.getDisplayMetrics()?.widthPixels ?: 0,
                screenHeight = context?.getResources()?.getDisplayMetrics()?.heightPixels ?: 0,
                screenOrientation = when (context.resources?.configuration?.orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> DeviceInformation.ScreenOrientation.Portrait
                    Configuration.ORIENTATION_LANDSCAPE -> DeviceInformation.ScreenOrientation.Landscape
                    else -> DeviceInformation.ScreenOrientation.Undefined
                },
                userAgent = userAgent
        )
    }
}