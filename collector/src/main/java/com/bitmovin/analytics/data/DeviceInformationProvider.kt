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

    fun getDeviceInformation(): DeviceInformation {

        val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()

        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val width = (displayMetrics.widthPixels / displayMetrics.density).roundToInt()
        val height = (displayMetrics.heightPixels / displayMetrics.density).roundToInt()

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
