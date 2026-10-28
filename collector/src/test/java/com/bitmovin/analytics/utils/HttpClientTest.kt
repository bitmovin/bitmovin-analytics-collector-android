package com.bitmovin.analytics.utils

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import java.io.IOException

class HttpClientTest {
    @Test
    fun postClosesTheResponseWhenTheCallbackThrows() {
        val okHttpCallback = slot<Callback>()
        val call = mockk<Call> { every { enqueue(capture(okHttpCallback)) } just runs }
        val okHttpClient = mockk<OkHttpClient> { every { newCall(any()) } returns call }
        val response = mockk<Response>(relaxed = true)
        val callback = mockk<Callback> { every { onResponse(any(), any()) } throws IOException("stream reset") }

        HttpClient(mockk(relaxed = true), okHttpClient).post("http://localhost/licensing", "{}", callback)

        assertThatThrownBy { okHttpCallback.captured.onResponse(call, response) }.isInstanceOf(IOException::class.java)
        verify { response.close() }
    }
}
