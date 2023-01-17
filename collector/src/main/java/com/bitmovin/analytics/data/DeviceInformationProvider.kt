package com.bitmovin.analytics.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.bitmovin.analytics.utils.Util

open class DeviceInformationProvider(val context: Context, val userAgent: String) {
    val isTV: Boolean = Util.isTVDevice(context)

    fun getDeviceInformation(): DeviceInformation {
        val windowManager: WindowManager? = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        val displayMetrics = DisplayMetrics()
        var width: Int = 0
        var height: Int = 0

        if (windowManager != null) {
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            width = displayMetrics.widthPixels
            height = displayMetrics.heightPixels

            // detecting UHD on androidTV requires to go over supported modes thus we limit this detection to TVs,
            // required API call only supported starting with v23
            if (isTV && Build.VERSION.SDK_INT >= 23) {
                val displaySize = getDisplaySizeV23(windowManager.defaultDisplay)

                // we make sure that workaround returns reasonable values
                if (displaySize.x > 0 && displaySize.y > 0) {
                    width = displaySize.x
                    height = displaySize.y
                }
            }
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
                Build.VERSION.SDK_INT >= 30 -> ">=8" // https://developer.amazon.com/docs/fire-tablets/fire-os-8.html#target-your-app-for-fire-os-8-devices
                Build.VERSION.SDK_INT >= 28 -> "7"
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

    @RequiresApi(23)
    private fun getDisplaySizeV23(display: Display): Point {
        // Detecting display size for TVs similar as ExoPlayer does it
        // (we also go over all modes additionally since I couldn't find anything about order of these modes in the API docs)
        // https://github.com/google/ExoPlayer/blob/3a654c1f54e19f261e717282fe42168b38d7e96c/library/common/src/main/java/com/google/android/exoplayer2/util/Util.java#L2761
        val modes: Array<Display.Mode> = display.supportedModes
        val displaySize = Point()

        if (modes.isNotEmpty()) {
            for (mode in modes) {
                // during emulation there was only 1 Mode, but to make this bulletproof we will
                // just look for the largest supported width mode which automatically favors horizontal orientation
                if (mode.physicalWidth > displaySize.x) {
                    displaySize.x = mode.physicalWidth
                    displaySize.y = mode.physicalHeight
                }
            }
        }
        return displaySize
    }
}
