/*
 * Copyright (C) 2023 Bitmovin Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Parts of this file are copied and adapted from https://github.com/google/ExoPlayer
 * The sections which are copied are marked with comments.
 */
package com.bitmovin.analytics.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.text.TextUtils
import android.text.TextUtils.split
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.bitmovin.analytics.features.FeatureManager.Companion.TAG
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

            // detecting UHD on androidTV requires some workaround
            // this is similar to what exoplayer is doing
            if (isTV) {
                val displaySize = getDisplaySizeOnTV(windowManager.defaultDisplay)

                // we make sure that workaround returns reasonable values
                if (displaySize.x > 0 && displaySize.y > 0) {
                    width = displaySize.x
                    height = displaySize.y
                }
            }
        }

        // os info is only set for devices with fire os to distinguish between android and fire os
        // we parse the useragent in ingress for this info in the default case
        var operatingSystem: String? = null
        var operatingSystemMajor: String? = null
        var deviceClass: DeviceClass? = if (isTV) DeviceClass.TV else null

        val packageManager = context.packageManager

        if (isFireOS(packageManager)) {
            operatingSystem = "Fire OS"
            operatingSystemMajor = fireOSVersion
            if (isFireTablet) {
                deviceClass = DeviceClass.Tablet
            } else if (isFireTV(packageManager)) {
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

    // This code is partly copied from exoplayer https://github.com/google/ExoPlayer and converted into Kotlin
    private fun getDisplaySizeOnTV(display: Display): Point {
        val displaySize = Point()

        // reading the displaysize from the sysproperty is not needed starting with Android S SDK (v31) according to android testcode
        // https://android.googlesource.com/platform/cts/+/master/tests/tests/display/src/android/display/cts/DisplayTest.java#750
        if (Build.VERSION.SDK_INT >= 31) {
            getDisplaySizeV23(display, displaySize)
            return displaySize
        }

        readDisplaySizeFromSysProperty(displaySize)

        // sanity check if returned values make sense
        if (displaySize.x > 0 && displaySize.y > 0) {
            return displaySize
        }

        if (Build.VERSION.SDK_INT >= 23) {
            getDisplaySizeV23(display, displaySize)
        } else if (Build.VERSION.SDK_INT >= 17) {
            getDisplaySizeV17(display, displaySize)
        }

        return displaySize
    }

    // This code is copied from exoplayer https://github.com/google/ExoPlayer, converted into kotlin and most comments are removed
    private fun readDisplaySizeFromSysProperty(outSize: Point) {
        val displaySizeString: String =
            if (Build.VERSION.SDK_INT < 28) {
                getSystemProperty("sys.display-size")
            } else {
                getSystemProperty("vendor.display-size")
            }

        if (!TextUtils.isEmpty(displaySizeString)) {
            try {
                val displaySizeParts = split(displaySizeString.trim(), "x")
                if (displaySizeParts.size == 2) {
                    val width = Integer.parseInt(displaySizeParts[0])
                    val height = Integer.parseInt(displaySizeParts[1])
                    if (width > 0 && height > 0) {
                        outSize.x = width
                        outSize.y = height
                        return
                    }
                }
            } catch (e: NumberFormatException) {
                // Do nothing.
            }
        }

        if ("Sony" == Build.MANUFACTURER &&
            Build.MODEL.startsWith("BRAVIA") &&
            context.packageManager.hasSystemFeature("com.sony.dtv.hardware.panel.qfhd")) {
            outSize.x = 3840
            outSize.y = 2160
            return
        }
    }

    // This code is copied from exoplayer https://github.com/google/ExoPlayer, converted into kotlin and adapted to iterate over all display modes
    @RequiresApi(23)
    private fun getDisplaySizeV23(display: Display, outSize: Point) {
        // Detecting display size for TVs similar as ExoPlayer does it
        // (we also go over all modes additionally since I couldn't find anything about order of these modes in the API docs)
        // https://github.com/google/ExoPlayer/blob/3a654c1f54e19f261e717282fe42168b38d7e96c/library/common/src/main/java/com/google/android/exoplayer2/util/Util.java#L2761
        val modes: Array<Display.Mode> = display.supportedModes

        if (modes.isNotEmpty()) {
            for (mode in modes) {
                // during emulation there was only 1 Mode, but to make this bulletproof we will
                // just look for the largest supported width mode which automatically favors horizontal orientation
                if (mode.physicalWidth > outSize.x) {
                    outSize.x = mode.physicalWidth
                    outSize.y = mode.physicalHeight
                }
            }
        }
    }

    // This code is copied from exoplayer https://github.com/google/ExoPlayer and converted into kotlin
    @RequiresApi(17)
    private fun getDisplaySizeV17(display: Display, outSize: Point) {
        display.getRealSize(outSize)
    }

    // This code is copied from exoplayer https://github.com/google/ExoPlayer and converted into kotlin
    private fun getSystemProperty(name: String): String {
        return try {
            @SuppressLint("PrivateApi")
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java)
            val property = getMethod.invoke(systemProperties::class.java, name) as String
            property
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read system property " + name, e)
            ""
        }
    }
}
