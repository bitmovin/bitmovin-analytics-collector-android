package com.bitmovin.analytics.persistence

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.CacheConsumingBackend
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.OnFailureCallback
import com.bitmovin.analytics.data.OnSuccessCallback
import com.bitmovin.analytics.persistence.queue.ConsumeOnlyAnalyticsEventQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private typealias Signal = Unit

internal class ConsumeOnlyPersistentCacheBackend(
    scope: CoroutineScope,
    private val backend: CallbackBackend,
    private val eventQueue: ConsumeOnlyAnalyticsEventQueue,
) : Backend, CacheConsumingBackend {

    // A channel that can only hold one element. Can be used
    // to conflate multiple signals into one.
    // Allows for self sustaining sequential flush loop as long the consuming code is blocking.
    private val cacheFlushChannel = Channel<Signal>(capacity = Channel.CONFLATED)

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
            cacheFlushChannel.trySend(Signal)
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
            cacheFlushChannel.trySend(Signal)
            success?.onSuccess()
        },
        failure,
    )

    override fun send(eventData: EventData) = send(eventData, null, null)

    override fun sendAd(eventData: AdEventData) = sendAd(eventData, null, null)

    private suspend fun sendNextCachedEvent() = eventQueue.popEvent()?.let { eventData ->
        sendSuspended(eventData)
    } ?: eventQueue.popAdEvent()?.let { adEventData ->
        sendAdSuspended(adEventData)
    } ?: false

    override fun startCacheFlushing() {
        cacheFlushChannel.trySend(Signal)
    }
}

private suspend fun CallbackBackend.sendSuspended(
    eventData: EventData,
): Boolean = suspendCoroutine { continuation ->
    val callback = ContinuationCallback(continuation)
    send(eventData, callback, callback)
}

private suspend fun CallbackBackend.sendAdSuspended(
    adEventData: AdEventData,
): Boolean = suspendCoroutine { continuation ->
    val callback = ContinuationCallback(continuation)
    sendAd(adEventData, callback, callback)
}

private class ContinuationCallback(
    private val continuation: Continuation<Boolean>,
) : OnFailureCallback, OnSuccessCallback {
    override fun onFailure(e: Exception, cancel: () -> Unit) {
        cancel()
        continuation.resume(false)
    }

    override fun onSuccess() {
        continuation.resume(true)
    }
}
