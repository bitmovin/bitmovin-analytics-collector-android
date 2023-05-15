package com.bitmovin.analytics.exoplayer.features

import com.bitmovin.analytics.exoplayer.features.ExoPlayerHttpRequestTrackingAdapter.Companion.extractStatusCode
import com.google.android.exoplayer2.source.LoadEventInfo
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ExoPlayerHttpRequestTrackingAdapterTest {

    @Test
    fun `extractStatusCode should return null on empty response headers map`() {
        val loadEventInfo = LoadEventInfo(1L, mockk(relaxed = true), 456L)
        assertThat(loadEventInfo.extractStatusCode).isNull()
    }

    @Test
    fun `extractStatusCode should return 200 on HTTP11 200 OK status`() {
        val loadEventInfo = LoadEventInfo(1L, mockk(relaxed = true), mockk(relaxed = true), LoadEventInfoTestUtils.getResponseHeaderMap("HTTP/1.1 200 OK"), 456L, 456L, 456L)
        assertThat(loadEventInfo.extractStatusCode).isEqualTo(200)
    }

    @Test
    fun `extractStatusCode should return 400 on HTTP11 400 BAD REQUEST status`() {
        val loadEventInfo = LoadEventInfo(1L, mockk(relaxed = true), mockk(relaxed = true), LoadEventInfoTestUtils.getResponseHeaderMap("HTTP/1.1 400 BAD REQUEST"), 456L, 456L, 456L)
        assertThat(loadEventInfo.extractStatusCode).isEqualTo(400)
    }

    @Test
    fun `extractStatusCode should return 404 on HTTP12 404 NOT FOUND status`() {
        val loadEventInfo = LoadEventInfo(1L, mockk(relaxed = true), mockk(relaxed = true), LoadEventInfoTestUtils.getResponseHeaderMap("HTTP/1.2 404 NOT FOUND"), 456L, 456L, 456L)
        assertThat(loadEventInfo.extractStatusCode).isEqualTo(404)
    }

    @Test
    fun `extractStatusCode should return null on missing http status`() {
        val loadEventInfo = LoadEventInfo(1L, mockk(relaxed = true), mockk(relaxed = true), LoadEventInfoTestUtils.getResponseHeaderMap(null), 456L, 456L, 456L)
        assertThat(loadEventInfo.extractStatusCode).isNull()
    }

    @Test
    fun `extractStatusCode should return null on wrong http status format`() {
        val loadEventInfo = LoadEventInfo(1L, mockk(relaxed = true), mockk(relaxed = true), LoadEventInfoTestUtils.getResponseHeaderMap("200 ok HTTP"), 456L, 456L, 456L)
        assertThat(loadEventInfo.extractStatusCode).isNull()
    }
}
