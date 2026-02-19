package com.bitmovin.analytics.data.cache

import com.bitmovin.analytics.dtos.EventData

internal data class SourceDataCache(
    val mpdUrl: String?,
    val m3u8Url: String?,
    val progUrl: String?,
    val streamFormat: String?,
    val isLive: Boolean,
    val videoDuration: Long = 0,
    val isCasting: Boolean = false,
    val castTech: String? = null,
    val videoPlaybackWidth: Int = 0,
    val videoPlaybackHeight: Int = 0,
    val videoBitrate: Int = 0,
    val audioBitrate: Int = 0,
    val videoTimeEnd: Long = 0,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val subtitleEnabled: Boolean = false,
    val subtitleLanguage: String? = null,
    val audioLanguage: String? = null,
) {
    companion object {
        fun fromEventData(eventData: EventData): SourceDataCache =
            SourceDataCache(
                mpdUrl = eventData.mpdUrl,
                m3u8Url = eventData.m3u8Url,
                progUrl = eventData.progUrl,
                isLive = eventData.isLive,
                isCasting = eventData.isCasting,
                castTech = eventData.castTech,
                videoDuration = eventData.videoDuration,
                videoPlaybackWidth = eventData.videoPlaybackWidth,
                videoPlaybackHeight = eventData.videoPlaybackHeight,
                videoBitrate = eventData.videoBitrate,
                audioBitrate = eventData.audioBitrate,
                videoTimeEnd = eventData.videoTimeEnd,
                streamFormat = eventData.streamFormat,
                videoCodec = eventData.videoCodec,
                audioCodec = eventData.audioCodec,
                subtitleEnabled = eventData.subtitleEnabled,
                subtitleLanguage = eventData.subtitleLanguage,
                audioLanguage = eventData.audioLanguage,
            )
    }
}
