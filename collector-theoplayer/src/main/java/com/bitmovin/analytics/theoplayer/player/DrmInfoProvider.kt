package com.bitmovin.analytics.theoplayer.player

internal class DrmInfoProvider {
    private var drmLicenseRequestTimeInMs: Long? = null

    @Synchronized
    fun reset() {
        drmLicenseRequestTimeInMs = null
    }

    @Synchronized
    fun getAndResetDrmLoadTime(): Long? {
        val result = drmLicenseRequestTimeInMs
        drmLicenseRequestTimeInMs = null
        return result
    }

    @Synchronized
    fun setDrmLicenseRequestTimeInMs(drmLicenseRequestTimeInMs: Long) {
        this.drmLicenseRequestTimeInMs = drmLicenseRequestTimeInMs
    }
}
