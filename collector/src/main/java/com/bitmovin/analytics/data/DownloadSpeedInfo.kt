package com.bitmovin.analytics.data

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
class DownloadSpeedInfo(
    // Number of completed segment downloads
    val segmentsDownloadCount: Int = 0,
    // Total download size in bytes
    val segmentsDownloadSize: Long = 0,
    // Total time spent downloading segments in milliseconds
    val segmentsDownloadTime: Long = 0,
    // Average download speed in kbps / null when there is no segment downloaded or not supported by the player
    val avgDownloadSpeed: Float? = null,
    // Maximum download speed in kbps / null when there is no segment downloaded or not supported by the player
    val maxDownloadSpeed: Float? = null,
    // Minimum download speed in kbps / null when there is no segment downloaded or not supported by the player
    val minDownloadSpeed: Float? = null,
    // Average time to first byte in milliseconds / null when there is no segment downloaded or not supported by the player
    val avgTimeToFirstByte: Float? = null,
)
