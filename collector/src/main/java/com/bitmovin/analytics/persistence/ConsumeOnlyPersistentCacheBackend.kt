package com.bitmovin.analytics.persistence

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.OnFailureCallback
import com.bitmovin.analytics.data.OnSuccessCallback
import com.bitmovin.analytics.persistence.queue.ConsumeOnlyAnalyticsEventQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class ConsumeOnlyPersistentCacheBackend(
    scope: CoroutineScope,
    private val backend: CallbackBackend,
    private val eventQueue: ConsumeOnlyAnalyticsEventQueue,
) : Backend, CallbackBackend {

    private val cacheFlushChannel = Channel<Boolean>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        cacheFlushChannel
            .consumeAsFlow()
            .onEach {
                sendNextCachedEvent()
            }
            .launchIn(scope)
    }

    override fun send(
        eventData: EventData,
        success: OnSuccessCallback?,
        failure: OnFailureCallback?,
    ) = backend.send(
        eventData,
        success = {
            cacheFlushChannel.trySend(true)
            success?.onSuccess()
        },
        failure,
    )

    override fun sendAd(
        eventData: AdEventData,
        success: OnSuccessCallback?,
        failure: OnFailureCallback?,
    ) = backend.sendAd(
        eventData,
        success = {
            cacheFlushChannel.trySend(true)
            success?.onSuccess()
        },
        failure,
    )

    override fun send(eventData: EventData) = send(eventData, null, null)

    override fun sendAd(eventData: AdEventData) = sendAd(eventData, null, null)

    private fun sendNextCachedEvent() {
        val eventData: EventData? = eventQueue.popEvent()
        eventData?.run {
            send(eventData)
        } ?: eventQueue.popAdEvent()?.let {
            sendAd(it)
        }
    }
}
