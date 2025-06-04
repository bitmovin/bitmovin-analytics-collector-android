package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

@Serializable
enum class DeviceClass(val value: String) {
    TV("TV"),
    Phone("Phone"),
    Other("Other"),
    Tablet("Tablet"),
    Wearable("Wearable"),
    Desktop("Desktop"),
    Console("Console"),
}
