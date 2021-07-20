package com.bitmovin.analytics.data

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.bitmovin.analytics.utils.Util
import kotlin.math.roundToInt

open class DeviceInformationProvider(val context: Context, val userAgent: String) {
    var isTV: Boolean = Util.isTVDevice(context)

    fun getDeviceInformation(): DeviceInformation {

        val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        var width: Int = 0
        var height: Int = 0

        if (windowManager != null) {
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            width = (displayMetrics.widthPixels / displayMetrics.density).roundToInt()
            height = (displayMetrics.heightPixels / displayMetrics.density).roundToInt()
        }

        return DeviceInformation(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                isTV = isTV,
                locale = Util.getLocale(),
                domain = Util.getDomain(context),
                screenWidth = width,
                screenHeight = height,
                userAgent = userAgent
        )
    }
}
