package com.bitmovin.analytics.test.utils

data class StreamData(
    val videoCodecStartsWith: String,
    val audioCodec: String?,
    val m3u8Url: String?,
    val progUrl: String?,
    val mpdUrl: String?,
    val streamFormat: String,
    val isLive: Boolean,
    val duration: Long,
    val drmSchema: String? = null,
    val drmLicenseUrl: String? = null,
    /**
     * The video bitrate ladder as defined in the manifest, sorted ascending (bits per second).
     *
     * For HLS these are the variant `EXT-X-STREAM-INF` `BANDWIDTH` values (peak bitrate of the
     * muxed video+audio variant); audio-only renditions are excluded. For DASH these are the
     * `bandwidth` values of the video `AdaptationSet` `Representation`s (video only).
     *
     * `null` for progressive single-file assets and streams without a reachable manifest. For
     * live streams this reflects the ladder observed at the time of writing and may change.
     */
    val videoBitrates: List<Int>? = null,
)
