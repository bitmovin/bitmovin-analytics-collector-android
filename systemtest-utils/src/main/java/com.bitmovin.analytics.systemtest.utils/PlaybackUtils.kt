package com.bitmovin.analytics.systemtest.utils

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions

object PlaybackUtils {
    fun waitUntil(condition: () -> Boolean) {
        val maxWaitMs = 15000L
        var waitingTotalMs = 0L
        val waitingDeltaMs = 100L
        val channel = Channel<Unit>()

        // TODO: when condition fails it is hard to track which condition caused the issue, we should add more logging here
        MainScope().launch {
            while (!condition()) {
                delay(waitingDeltaMs)
                waitingTotalMs += waitingDeltaMs

                if (waitingTotalMs >= maxWaitMs) {
                    Assertions.fail<Nothing>("expected condition wasn't fulfilled within $maxWaitMs ms")
                }
            }

            channel.send(Unit)
        }

        runBlocking {
            channel.receive()
        }

        channel.close()
    }
}
