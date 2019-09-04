package com.bitmovin.analytics.data

import android.os.Build

class DeviceInformation {
    val manufacturer: String = Build.MANUFACTURER
    val model: String = Build.MODEL
}