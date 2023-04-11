package com.bitmovin.analytics.systemtest.utils

object TestSamples {
    val HLS_REDBULL = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8",
        "hls",
        false,
        210000,
    )

    val DASH = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd",
        "dash",
        false,
        210000,
    )

    val PROGRESSIVE = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/MI201109210084_mpeg-4_hd_high_1080p25_10mbits.mp4",
        "progressive",
        false,
        210000,
    )

    val IVS_LIVE_1 = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.DmumNckWFTqz.m3u8",
        "hls",
        true,
        -1,
    )

    val IVS_LIVE_2 = StreamData(
        "avc1",
        "mp4a.40.2",
        "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.XFAcAcypUxQm.m3u8",
        "hls",
        true,
        -1,
    )

    val IVS_VOD_1 = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://d6hwdeiig07o4.cloudfront.net/ivs/956482054022/cTo5UpKS07do/2020-07-13T22-54-42.188Z/OgRXMLtq8M11/media/hls/master.m3u8",
        "hls",
        false,
        362356,
    )
}
