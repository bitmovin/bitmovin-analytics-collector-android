package com.bitmovin.analytics.utils

import com.bitmovin.analytics.api.LogLevel

/**
 * Internal storage of the configuration for the logging behavior of the SDK
 * This stores the setting from the config of the collector.
 */
internal object LogLevelConfig {
    var logLevel = LogLevel.ERROR
}
