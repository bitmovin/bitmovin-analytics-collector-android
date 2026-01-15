package com.bitmovin.analytics.theoplayer.player

internal fun Double.convertDoubleSecondsToLongMs(): Long {
    val ms = this * 1000
    return ms.toLong()
}
