package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.data.AdModuleInformation

interface AdAdapter {
    fun release()
    val moduleInformation: AdModuleInformation
}