package com.bitmovin.analytics.data

import android.content.Context
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
                userAgent = userAgent
        )
    }
}
