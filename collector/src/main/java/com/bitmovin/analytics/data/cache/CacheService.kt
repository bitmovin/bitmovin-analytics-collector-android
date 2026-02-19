package com.bitmovin.analytics.data.cache

import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.internal.InternalBitmovinApi

/*
    There can be cases where the player is already destroyed or a new source is already loaded, but we still
    want to send out a closing sample of the old session. Thus we store the source data a cache, and use
    this to provide the data.
 */
@InternalBitmovinApi
class CacheService {
    private var sourceCache: SourceDataCache? = null
    private var dynamicSourceDataCache: DynamicSourceDataCache = DynamicSourceDataCache()

    @Synchronized
    fun setSourceCacheFromEventData(eventData: EventData) {
        sourceCache = SourceDataCache.fromEventData(eventData)
        dynamicSourceDataCache.videoTimeEnd = eventData.videoTimeEnd
    }

    @Synchronized
    fun setVideoTimeEnd(videoTimeEnd: Long) {
        dynamicSourceDataCache.videoTimeEnd = videoTimeEnd
    }

    @Synchronized
    fun applyCacheOnEventData(eventData: EventData) {
        val sourceCache = sourceCache

        if (sourceCache != null) {
            eventData.mpdUrl = sourceCache.mpdUrl
            eventData.m3u8Url = sourceCache.m3u8Url
            eventData.progUrl = sourceCache.progUrl
            eventData.isLive = sourceCache.isLive
            eventData.isCasting = sourceCache.isCasting
            eventData.castTech = sourceCache.castTech
            eventData.videoDuration = sourceCache.videoDuration
            eventData.videoPlaybackWidth = sourceCache.videoPlaybackWidth
            eventData.videoPlaybackHeight = sourceCache.videoPlaybackHeight
            eventData.videoBitrate = sourceCache.videoBitrate
            eventData.audioBitrate = sourceCache.audioBitrate
            eventData.streamFormat = sourceCache.streamFormat
            eventData.videoCodec = sourceCache.videoCodec
            eventData.audioCodec = sourceCache.audioCodec
            eventData.subtitleEnabled = sourceCache.subtitleEnabled
            eventData.subtitleLanguage = sourceCache.subtitleLanguage
            eventData.audioLanguage = sourceCache.audioLanguage
        }

        // only update videoTimeEnd in case it is not null and more than
        // the one reported by eventData, this is best effort, but should work in most cases
        val cachedVideoTimeEnd = dynamicSourceDataCache.videoTimeEnd
        if (cachedVideoTimeEnd != null && cachedVideoTimeEnd > eventData.videoTimeEnd) {
            eventData.videoTimeEnd = cachedVideoTimeEnd
        }
    }

    @Synchronized
    fun resetSourceCache() {
        sourceCache = null
        dynamicSourceDataCache.videoTimeEnd = null
    }
}
