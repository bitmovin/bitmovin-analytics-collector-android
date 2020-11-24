package com.bitmovin.analytics

import com.bitmovin.analytics.data.EventDataDecorator

interface EventDataDecoratorPipeline {

    fun registerEventDataDecorator(decorator: EventDataDecorator)
}
