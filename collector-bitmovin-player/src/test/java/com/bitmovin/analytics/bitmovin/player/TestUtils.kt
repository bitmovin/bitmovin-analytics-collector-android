package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import io.mockk.mockk

class TestUtils {
    companion object {
        // helper to create a context mock for the java tests
        fun createMockContext(): Context {
            return mockk(relaxed = true)
        }
    }
}
