package com.bitmovin.analytics.data

interface CacheConsumingBackend : CallbackBackend {
    fun startCacheFlushing()
}