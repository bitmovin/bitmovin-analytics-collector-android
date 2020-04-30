package com.bitmovin.analytics.data

data class DeviceInformation(
        val manufacturer: String,
        val model: String,
        val isTV: Boolean,
        val userAgent: String,
        val locale: String,
        val packageName: String,
        val screenHeight: Int,
        val screenWidth: Int
)
