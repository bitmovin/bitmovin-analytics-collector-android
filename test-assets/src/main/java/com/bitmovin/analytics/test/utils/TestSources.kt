package com.bitmovin.analytics.test.utils

@Suppress("ktlint:standard:max-line-length")
object TestSources {
    val HLS_REDBULL =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            "https://bitmovin-player-eu-west1-ci-input.s3.amazonaws.com/general/aom/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8",
            null,
            null,
            "hls",
            false,
            210000,
        )

    val THEO_BIGBUCKBUNNY =
        StreamData(
            "avc1.",
            null,
            null,
            null,
            "https://cdn.theoplayer.com/video/dash/big_buck_bunny/BigBuckBunny_10s_simple_2014_05_09.mpd",
            "dash",
            false,
            596460,
        )

    val DASH =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            null,
            null,
            "https://bitmovin-player-eu-west1-ci-input.s3.amazonaws.com/general/hls/art-of-motion/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd",
            "dash",
            false,
            210000,
        )

    val DASH_LIVE =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            null,
            null,
            "https://livesim.dashif.org/livesim2/testpic_2s/Manifest.mpd",
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
    val DASH_SINTEL_WITH_SUBTITLES =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            null,
            null,
            "https://bitmovin-player-eu-west1-ci-input.s3.amazonaws.com/general/sintel/sintel_single_quality.mpd",
            "dash",
            false,
            888000,
        )

    /**
     * Source with two audio tracks with two qualities each as well as four subtitle tracks.
     * Audio languages: [en, dubbing]
     * Audio qualities: [stereo, surround]
     */
    val HLS_MULTIPLE_AUDIO_LANGUAGES =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            "https://bitmovin-player-eu-west1-ci-input.s3.amazonaws.com/general/sintel/hls/playlist.m3u8",
            null,
            null,
            "hls",
            false,
            888000,
        )

    val PROGRESSIVE =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            null,
            "https://bitmovin-player-eu-west1-ci-input.s3.amazonaws.com/general/hls/art-of-motion/MI201109210084_mpeg-4_hd_high_1080p25_10mbits.mp4",
            null,
            "progressive",
            false,
            210304,
        )

    val IVS_LIVE_1 =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/" +
                "us-west-2.893648527354.channel.DmumNckWFTqz.m3u8",
            null,
            null,
            "hls",
            true,
            0,
        )

    val IVS_LIVE_2 =
        StreamData(
            "avc1",
            "mp4a.40.2",
            "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/" +
                "us-west-2.893648527354.channel.XFAcAcypUxQm.m3u8",
            null,
            null,
            "hls",
            true,
            0,
        )

    val IVS_VOD_1 =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            "https://d6hwdeiig07o4.cloudfront.net/ivs/956482054022/cTo5UpKS07do/" +
                "2020-07-13T22-54-42.188Z/OgRXMLtq8M11/media/hls/master.m3u8",
            null,
            null,
            "hls",
            false,
            362356,
        )

    val BBB =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            null,
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/mp4/BigBuckBunny.mp4",
            null,
            "progressive",
            false,
            596000,
        )

    val CORRUPT_DASH =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            null,
            null,
            "https://bitmovin-player-eu-west1-ci-input.s3-eu-west-1.amazonaws.com/general/dash/corrupted_segment/corrupted_first_segment.mpd",
            "dash",
            false,
            0,
        )

    val MISSING_SEGMENT =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            null,
            null,
            "https://storage.googleapis.com/bitmovin-analytics-test-assets/corrupted-assets/redbull-parkour/stream_segment_not_found.mpd",
            "dash",
            false,
            0,
        )

    val NONE_EXISTING_STREAM =
        StreamData(
            "",
            null,
            "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.DmumNckWFTqz_invalid.m3u8",
            null,
            null,
            "hls",
            false,
            0,
        )

    val DRM_DASH_WIDEVINE =
        StreamData(
            "avc1.",
            "mp4a.40.2",
            null,
            null,
            "https://bitmovin-player-eu-west1-ci-input.s3.amazonaws.com/general/hls/art-of-motion-aes-128-multivariant/mpds/11331.mpd",
            "dash",
            false,
            210000,
            drmSchema = "widevine",
            drmLicenseUrl = "https://cwip-shaka-proxy.appspot.com/no_auth",
        )

    // These are IMA Sample Tags from https://developers.google.com/interactive-media-ads/docs/sdks/android/tags
    val IMA_AD_SOURCE_1 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirecterror&nofb=1&correlator="
    val IMA_AD_SOURCE_2 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator="
    val IMA_AD_SOURCE_3 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator="
    val IMA_AD_SOURCE_4 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirectlinear&correlator="

    val IMA_VMAP_MIDROLL_2ADS = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_skip_ad_samples&sz=640x480&cust_params=sample_ar%3Dmidskiponly&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&cmsid=496&vid=short_onecue&correlator="

    // Standard VAST tag for THEOplayer's built-in CSAI ad player (no Google IMA required)
    val THEO_AD_VAST_SOURCE = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator="
}
