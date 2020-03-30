package com.bitmovin.analytics.data

data class DeviceInformation(val manufacturer: String,
                             val model: String,
                             val userAgent: String,
                             val locale: String,
                             val packageName: String,
                             val screenHeight: Int,
                             val screenWidth: Int,
                             val screenOrientation: ScreenOrientation
) {
    enum class ScreenOrientation(val value: String?) {
        Landscape("landscape"),
        Portrait("portrait"),
        Undefined(null)
    }
}

