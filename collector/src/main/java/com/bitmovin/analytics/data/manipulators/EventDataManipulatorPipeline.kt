package com.bitmovin.analytics.data.manipulators

interface EventDataManipulatorPipeline {
    fun clearEventDataManipulators()
    fun registerEventDataManipulator(manipulator: EventDataManipulator)
}
