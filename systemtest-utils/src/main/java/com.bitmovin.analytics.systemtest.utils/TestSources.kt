package com.bitmovin.analytics.systemtest.utils

object TestSources {
    val HLS_REDBULL = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8",
        null,
        null,
        "hls",
        false,
        210000,
    )

    val DASH = StreamData(
        "avc1.",
        "mp4a.40.2",
        null,
        null,
        "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd",
        "dash",
        false,
        210000,
    )

    val DASH_LIVE = StreamData(
        "avc1.",
        "mp4a.40.2",
        null,
        null,
        "https://livesim.dashif.org/livesim/testpic_2s/Manifest.mpd",
        "dash",
        true,
        0,
    )

    /**
     * Sintel
     * Source with 5 subtitle tracks
     * Audio language: en_stereo
     * Subtitle languages: [en, de, es, fr, img]
     */
    val DASH_SINTEL_WITH_SUBTITLES = StreamData(
        "avc1.",
        "mp4a.40.2",
        null,
        null,
        "https://bitmovin-a.akamaihd.net/content/sintel/sintel_single_quality.mpd",
        "dash",
        false,
        888000,
    )

    /**
     * Source with two audio tracks with two qualities each as well as four subtitle tracks.
     * Audio languages: [en, dubbing]
     * Audio qualities: [stereo, surround]
     */
    val HLS_MULTIPLE_AUDIO_LANGUAGES = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
        null,
        null,
        "hls",
        false,
        888000,
    )

    val PROGRESSIVE = StreamData(
        "avc1.",
        "mp4a.40.2",
        null,
        "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/MI201109210084_mpeg-4_hd_high_1080p25_10mbits.mp4",
        null,
        "progressive",
        false,
        210304,
    )

    val IVS_LIVE_1 = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.DmumNckWFTqz.m3u8",
        null,
        null,
        "hls",
        true,
        0,
    )

    val IVS_LIVE_2 = StreamData(
        "avc1",
        "mp4a.40.2",
        "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.XFAcAcypUxQm.m3u8",
        null,
        null,
        "hls",
        true,
        0,
    )

    val IVS_VOD_1 = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://d6hwdeiig07o4.cloudfront.net/ivs/956482054022/cTo5UpKS07do/2020-07-13T22-54-42.188Z/OgRXMLtq8M11/media/hls/master.m3u8",
        null,
        null,
        "hls",
        false,
        362356,
    )

    val DRM_DASH_WIDEVINE = StreamData(
        "avc1.",
        "mp4a.40.2",
        null,
        null,
        "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd",
        "dash",
        false,
        210000,
        drmSchema = "widevine",
        drmLicenseUrl = "https://cwip-shaka-proxy.appspot.com/no_auth",
    )

    val DRM_HLS_WIDEVINE = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/m3u8s/widevine-hls.m3u8",
        null,
        null,
        "hls",
        false,
        210000,
        drmSchema = "widevine",
        drmLicenseUrl = "https://cwip-shaka-proxy.appspot.com/no_auth",
    )

    // TODO: what does encrypted here mean?? vs DRM only?
    val DRM_HLS_WIDEVINE_ENCRYPTED = StreamData(
        "avc1.",
        "mp4a.40.2",
        "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/m3u8s/11331.m3u8",
        null,
        null,
        "hls",
        false,
        210000,
        drmSchema = "widevine",
        drmLicenseUrl = "https://widevine-proxy.appspot.com/proxy",
    )
}
