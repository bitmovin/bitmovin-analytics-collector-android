package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.data.AdModuleInformation

interface AdAdapter : Observable<AdAnalyticsEventListener> {
    fun release()
    val isLinearAdActive: Boolean
    val moduleInformation: AdModuleInformation
    val isAutoplayEnabled: Boolean?
}
