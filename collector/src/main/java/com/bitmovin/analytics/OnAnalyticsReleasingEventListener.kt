package com.bitmovin.analytics

import com.bitmovin.analytics.internal.InternalBitmovinApi

@InternalBitmovinApi
interface OnAnalyticsReleasingEventListener {
    fun onReleasing()
}
