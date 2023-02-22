package com.bitmovin.analytics.data

import androidx.annotation.Keep

@Keep // Protect from obfuscation in case customers are using proguard
class DownloadSpeedInfo {

    // Number of completed segment downloads
    var segmentsDownloadCount: Int = 0

    // Total download size in bytes
    var segmentsDownloadSize: Long = 0

    // Total time spent downloading segments in milliseconds
    var segmentsDownloadTime: Long = 0

    // Average download speed in kbps
    var avgDownloadSpeed: Float? = 0.0f

    // Maximum download speed in kbps
    var maxDownloadSpeed: Float? = 0.0f

    // Minimum download speed in kbps
    var minDownloadSpeed: Float? = 0.0f

    // Average time to first byte in milliseconds
    var avgTimeToFirstByte: Float? = 0.0f
}
