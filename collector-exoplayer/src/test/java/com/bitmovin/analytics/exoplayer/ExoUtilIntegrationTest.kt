package com.bitmovin.analytics.exoplayer

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(
    RobolectricTestRunner::class,
)
class ExoUtilIntegrationTest {

    @Test
    @Config(sdk = [33])
    fun `executeSyncOrAsyncOnLooperThread should execute code sync`() {
        // arrange
        val atomicInt = AtomicInteger(0)

        // act
        ExoUtil.executeSyncOrAsyncOnLooperThread(Looper.getMainLooper()) {
            Thread.sleep(1)
            atomicInt.incrementAndGet()
        }

        // assert
        assertThat(atomicInt.get()).isEqualTo(1)
    }

    @Test
    @Config(sdk = [33])
    fun `executeSyncOrAsyncOnLooperThread should execute code async`() {
        // arrange
        val atomicInt = AtomicInteger(0)

        // we run code in non main thread to test async execution
        val thread = HandlerThread("HandlerThread")
        thread.start()
        val handler = Handler(thread.looper)

        // act
        ExoUtil.executeSyncOrAsyncOnLooperThread(handler.looper) {
            Thread.sleep(1)
            atomicInt.incrementAndGet()
        }

        // assert
        assertThat(atomicInt.get()).isEqualTo(0)
        Thread.sleep(10)
        assertThat(atomicInt.get()).isEqualTo(1)
    }
}
