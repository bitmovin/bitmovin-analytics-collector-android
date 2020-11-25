package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.EventDataDecoratorPipeline
import com.bitmovin.analytics.data.DRMInformation

interface PlayerAdapter {
    fun init()
    fun release()
    fun registerEventDataDecorators(pipeline: EventDataDecoratorPipeline?)
    val position: Long
    val dRMInformation: DRMInformation?
    fun clearValues()
}