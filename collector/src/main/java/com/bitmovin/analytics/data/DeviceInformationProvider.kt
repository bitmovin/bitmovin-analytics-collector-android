package com.bitmovin.analytics.data

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.bitmovin.analytics.utils.Util

open class DeviceInformationProvider(val context: Context, val userAgent: String) {
    private var isTV: Boolean = false
    init {
        isTV = Util.isTVDevice(context)
    }
    fun getDeviceInformation(): DeviceInformation {
        return DeviceInformation(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                isTV = isTV,
                locale = Util.getLocale(),
                packageName = context.packageName,
                screenWidth = context?.getResources()?.getDisplayMetrics()?.widthPixels ?: 0,
                screenHeight = context?.getResources()?.getDisplayMetrics()?.heightPixels ?: 0,
                userAgent = userAgent
        )
    }
}
