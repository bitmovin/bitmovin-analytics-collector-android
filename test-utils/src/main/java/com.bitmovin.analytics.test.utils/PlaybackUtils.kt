package com.bitmovin.analytics.test.utils

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions

object PlaybackUtils {
    fun waitUntil(
        conditionName: String = "",
        condition: () -> Boolean,
    ) {
        val maxWaitMs = 45000L
        var waitingTotalMs = 0L
        val waitingDeltaMs = 100L
        // the condition is evaluated on the main thread, but the result needs to be
        // reported back to the test thread, since an exception thrown inside the
        // MainScope coroutine crashes the app instead of failing the test
        val channel = Channel<Result<Boolean>>()

        MainScope().launch {
            val conditionFulfilled =
                runCatching {
                    while (!condition()) {
                        delay(waitingDeltaMs)
                        waitingTotalMs += waitingDeltaMs

                        if (waitingTotalMs >= maxWaitMs) {
                            return@runCatching false
                        }
                    }
                    true
                }

            channel.send(conditionFulfilled)
        }

        val conditionFulfilled =
            runBlocking {
                channel.receive()
            }

        channel.close()

        if (!conditionFulfilled.getOrThrow()) {
            Assertions.fail<Nothing>("expected condition ($conditionName) wasn't fulfilled within $maxWaitMs ms")
        }
    }
}
