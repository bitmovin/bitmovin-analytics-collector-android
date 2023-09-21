package com.bitmovin.analytics.exoplayer.player

import com.bitmovin.analytics.enums.DRMType
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.drm.DrmInitData
import com.google.android.exoplayer2.source.MediaLoadData

internal class DrmInfoProvider {
    private var drmLoadStartTime: Long = 0

    var drmDownloadTime: Long? = null

    var drmType: String? = null
        private set

    fun reset() {
        drmLoadStartTime = 0
        drmDownloadTime = null
        drmType = null
    }

    fun drmLoadStartedAt(loadStartedAtMs: Long) {
        this.drmLoadStartTime = loadStartedAtMs
    }
    fun drmLoadFinishedAt(loadFinishedAtMs: Long) {
        this.drmDownloadTime = loadFinishedAtMs - drmLoadStartTime
    }

    fun evaluateDrmType(mediaLoadData: MediaLoadData) {
        var drmType: String? = null
        var i = 0
        val drmInitData = mediaLoadData.trackFormat?.drmInitData
        if (drmInitData != null) {
            while (drmType == null && i < drmInitData.schemeDataCount) {
                val data = drmInitData.get(i)
                drmType = getDrmTypeFromSchemeData(data)
                i++
            }
        }
        this.drmType = drmType
    }

    private fun getDrmTypeFromSchemeData(data: DrmInitData.SchemeData?): String? {
        data ?: return null
        return when {
            data.matches(C.WIDEVINE_UUID) -> DRMType.WIDEVINE.value
            data.matches(C.CLEARKEY_UUID) -> DRMType.CLEARKEY.value
            data.matches(C.PLAYREADY_UUID) -> DRMType.PLAYREADY.value
            else -> null
        }
    }
}
