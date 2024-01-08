package com.bitmovin.analytics.systemtest.utils

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
        val channel = Channel<Unit>()

        MainScope().launch {
            while (!condition()) {
                delay(waitingDeltaMs)
                waitingTotalMs += waitingDeltaMs

                if (waitingTotalMs >= maxWaitMs) {
                    Assertions.fail<Nothing>("expected condition ($conditionName) wasn't fulfilled within $maxWaitMs ms")
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
