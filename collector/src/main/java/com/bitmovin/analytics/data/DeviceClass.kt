package com.bitmovin.analytics.data

import androidx.annotation.Keep

@Keep // Protect from obfuscation in case customers are using proguard, enums might not be strictly needed but there could be rules that mess up with our dtos
enum class DeviceClass {
    TV,
    Phone,
    Other,
    Tablet,
    Wearable,
    Desktop,
    Console,
}
