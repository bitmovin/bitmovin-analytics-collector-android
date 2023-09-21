package com.bitmovin.analytics.exoplayer.player

import com.google.android.exoplayer2.C.CLEARKEY_UUID
import com.google.android.exoplayer2.C.DATA_TYPE_DRM
import com.google.android.exoplayer2.C.DATA_TYPE_MANIFEST
import com.google.android.exoplayer2.C.PLAYREADY_UUID
import com.google.android.exoplayer2.C.WIDEVINE_UUID
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.drm.DrmInitData
import com.google.android.exoplayer2.source.MediaLoadData
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DrmInfoProviderTest {

    private lateinit var provider: DrmInfoProvider

    @Before
    fun setup() {
        provider = DrmInfoProvider()
    }

    @Test
    fun `fields should be null on init`() {
        assertEquals(null, provider.drmDownloadTime)
        assertEquals(null, provider.drmType)
    }

    @Test
    fun `drmDownloadTime should be calculated correctly`() {
        provider.drmLoadStartedAt(10)
        provider.drmLoadFinishedAt(50)

        assertEquals(40L, provider.drmDownloadTime)
    }

    @Test
    fun `reset should reset fields`() {
        provider.drmLoadStartedAt(10)
        provider.drmLoadFinishedAt(50)

        provider.reset()

        assertEquals(null, provider.drmDownloadTime)
        assertEquals(null, provider.drmType)
    }

    @Test
    fun `evaluateDrmType should ignore data type manifest`() {
        val mediaLoaded = MediaLoadData(DATA_TYPE_MANIFEST)
        provider.evaluateDrmType(mediaLoaded)
        assertEquals(null, provider.drmType)
    }

    @Test
    fun `evaluateDrmType should detect widevine`() {
        val schemeData = DrmInitData.SchemeData(WIDEVINE_UUID, "mimeType", byteArrayOf())
        val drmInitData = DrmInitData(schemeData)
        val format = Format.Builder()
            .setDrmInitData(drmInitData)
            .build()
        val mediaLoadData = MediaLoadData(DATA_TYPE_DRM, 0, format, 0, null, 0, 0)
        provider.evaluateDrmType(mediaLoadData)
        assertEquals("widevine", provider.drmType)
    }

    @Test
    fun `evaluateDrmType should detect playready`() {
        val schemeData = DrmInitData.SchemeData(PLAYREADY_UUID, "mimeType", byteArrayOf())
        val drmInitData = DrmInitData(schemeData)
        val format = Format.Builder()
            .setDrmInitData(drmInitData)
            .build()
        val mediaLoadData = MediaLoadData(DATA_TYPE_DRM, 0, format, 0, null, 0, 0)
        provider.evaluateDrmType(mediaLoadData)
        assertEquals("playready", provider.drmType)
    }

    @Test
    fun `evaluateDrmType should detect clearkey`() {
        val schemeData = DrmInitData.SchemeData(CLEARKEY_UUID, "mimeType", byteArrayOf())
        val drmInitData = DrmInitData(schemeData)
        val format = Format.Builder()
            .setDrmInitData(drmInitData)
            .build()
        val mediaLoadData = MediaLoadData(DATA_TYPE_DRM, 0, format, 0, null, 0, 0)
        provider.evaluateDrmType(mediaLoadData)
        assertEquals("clearkey", provider.drmType)
    }
}
