package com.bitmovin.analytics.data.manipulators

interface EventDataManipulatorPipeline {

    fun registerEventDataManipulator(manipulator: EventDataManipulator)
}
