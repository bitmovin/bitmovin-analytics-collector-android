package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.Observable

interface AdAdapter : Observable<AdAnalyticsEventListener> {
    fun release()

    val isLinearAdActive: Boolean
}
