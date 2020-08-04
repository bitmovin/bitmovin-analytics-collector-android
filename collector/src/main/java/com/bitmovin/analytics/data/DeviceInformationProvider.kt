package com.bitmovin.analytics.data

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.bitmovin.analytics.utils.Util
import kotlin.math.roundToInt

open class DeviceInformationProvider(val context: Context, val userAgent: String) {
    var isTV: Boolean = isTVDevice()

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()

    val res = windowManager.defaultDisplay.getMetrics(displayMetrics)
    val configuration: Configuration = context.resources.configuration

    val width = (configuration.screenWidthDp * displayMetrics.density).roundToInt()
    val height = (configuration.screenHeightDp * displayMetrics.density).roundToInt()

    fun getDeviceInformation(): DeviceInformation {
        return DeviceInformation(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                isTV = isTV,
                locale = Util.getLocale(),
                packageName = context.packageName,
                screenWidth = width,
                screenHeight = height,
                userAgent = userAgent
        )
    }

    private fun isTVDevice(): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
