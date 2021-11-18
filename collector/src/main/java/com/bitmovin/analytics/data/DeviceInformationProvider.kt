package com.bitmovin.analytics.data

import android.content.Context
import android.content.pm.PackageManager
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

        var operatingSystem: String? = null
        var operatingSystemMajor: String? = null
        val operatingSystemMinor: String? = null
        var deviceClass: DeviceClass? = if (isTV) DeviceClass.TV else null

        if (isFireOS(context.packageManager)) {
            operatingSystem = "Fire OS"
            operatingSystemMajor = fireOSVersion
            if (isFireTablet) {
                deviceClass = DeviceClass.Tablet
            } else if (isFireTV(context.packageManager)) {
                deviceClass = DeviceClass.TV
            }
        }

        return DeviceInformation(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                isTV = isTV,
                locale = Util.getLocale(),
                domain = Util.getDomain(context),
                screenWidth = width,
                screenHeight = height,
                userAgent = userAgent,
                operatingSystem = operatingSystem,
                operatingSystemMajor = operatingSystemMajor,
                operatingSystemMinor = operatingSystemMinor,
                deviceClass = deviceClass
        )
    }

    companion object {
        private const val AMAZON_FEATURE_FIRE_TV = "amazon.hardware.fire_tv"

        fun isFireOS(packageManager: PackageManager): Boolean {
            return isFireTV(packageManager) || isFireTablet
        }

        private val fireOSVersion: String
            get() = when {
                Build.VERSION.SDK_INT >= 28 -> ">=7"
                Build.VERSION.SDK_INT >= 25 -> "6"
                Build.VERSION.SDK_INT >= 22 -> "5"
                Build.VERSION.SDK_INT >= 19 -> "4"
                else -> "Unknown"
            }

        // This will also include FireTV Sticks
        // https://developer.amazon.com/docs/fire-tv/identify-amazon-fire-tv-devices.html
        private fun isFireTV(packageManager: PackageManager) = packageManager.hasSystemFeature(AMAZON_FEATURE_FIRE_TV)

        // https://developer.amazon.com/docs/fire-tablets/ft-identifying-tablet-devices.html
        private val isFireTablet: Boolean
            get() = "Amazon".equals(Build.MANUFACTURER, true) && Build.MODEL?.startsWith("KF", true) == true
    }
}
