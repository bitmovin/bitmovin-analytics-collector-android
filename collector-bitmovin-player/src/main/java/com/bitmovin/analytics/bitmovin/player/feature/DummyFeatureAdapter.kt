package com.bitmovin.analytics.bitmovin.player.feature

import android.util.Log
import com.bitmovin.analytics.features.DummyEvent
import com.bitmovin.analytics.features.DummyFeatureAdapter
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener

class BitmovinDummyFeatureAdapter(val player: BitmovinPlayer): DummyFeatureAdapter() {
    companion object {
        val TAG = BitmovinDummyFeatureAdapter::class.simpleName
    }

    private val onDownloadFinishedListener = OnDownloadFinishedListener { downloadFinishedEvent ->
        on {it.onDummyEvent(DummyEvent("test"))}
    }

    init {
        player.addEventListener(onDownloadFinishedListener)
    }

    override fun twoWayCommunication() {
        Log.d(TAG, "Two way communication test")
    }

    override fun dispose() {
        player.removeEventListener(onDownloadFinishedListener)
    }
}
