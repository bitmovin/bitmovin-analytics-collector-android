package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.data.AdModuleInformation

interface AdAdapter {
    fun release()
    val isLinearAdActive: Boolean
    val moduleInformation: AdModuleInformation
    val isAutoplayEnabled: Boolean?
}
