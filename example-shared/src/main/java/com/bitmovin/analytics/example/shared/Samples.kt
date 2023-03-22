package com.bitmovin.analytics.example.shared

// https://github.com/google/ExoPlayer/blob/release-v1/demo/src/main/java/com/google/android/exoplayer/demo/Samples.java
// https://github.com/google/ExoPlayer/blob/release-v2/demos/main/src/main/assets/media.exolist.json
object Samples {
    val HLS = Sample("Hls", "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8")
    val DASH = Sample("Dash", "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd")
    val SMOOTH = Sample("Smooth", "https://test.playready.microsoft.com/smoothstreaming/SSWSS720H264/SuperSpeedway_720.ism/manifest")
    val PROGRESSIVE = Sample("Progressive", "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/MI201109210084_mpeg-4_hd_high_1080p25_10mbits.mp4")

    val HLS_DRM_WIDEVINE = Sample("Hls + Widevine", "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/m3u8s/widevine-hls.m3u8", "widevine", "https://widevine-proxy.appspot.com/proxy")
    val HLS_DRM_WIDEVINE_ENCRYPTED = Sample("Encrypted Hls + Widevine", "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/m3u8s/11331.m3u8", "widevine", "https://widevine-proxy.appspot.com/proxy")
    val DASH_DRM_WIDEVINE = Sample("Dash + Widevine", "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd", "widevine", "https://cwip-shaka-proxy.appspot.com/no_auth")

    val HLS_REDBULL = Sample("HLSRedbulll", "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8")
    val DASH_LIVE = Sample("DashLive", "https://livesim.dashif.org/livesim/testpic_2s/Manifest.mpd")
    val DASH_SINTEL = Sample("DashSintel", "https://bitmovin-a.akamaihd.net/content/sintel/sintel.mpd")
    val BBB = Sample("BBB", "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/mp4/BigBuckBunny.mp4")

    // Dash stream with corrupted first sample
    val CORRUPT_DASH = Sample("CorruptRedBull", "https://bitmovin-a.akamaihd.net/content/analytics-teststreams/redbull-parkour/corrupted_first_segment.mpd")
    val MISSING_SEGMENT = Sample("RedBullMissingSegment", "https://storage.googleapis.com/bitmovin-analytics-test-assets/corrupted-assets/redbull-parkour/stream_segment_not_found.mpd")

    // These are IMA Sample Tags from https://developers.google.com/interactive-media-ads/docs/sdks/android/tags
    val IMA_AD_SOURCE_1 = Sample("ImaAdSource1", "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirecterror&nofb=1&correlator=")
    val IMA_AD_SOURCE_2 = Sample("ImaAdSource2", "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator=")
    val IMA_AD_SOURCE_3 = Sample("ImaAdSource3", "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator=")
    val IMA_AD_SOURCE_4 = Sample("ImaAdSource4", "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirectlinear&correlator=")

    // IVS example sources
    val ivsLiveStream1Source = Sample("liveStream1Source", "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.DmumNckWFTqz.m3u8")
    val ivsLiveStream2Source = Sample("liveStream2Source", "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.XFAcAcypUxQm.m3u8")
    val ivsVodStreamSource = Sample("ivsVodStreamSource", "https://d6hwdeiig07o4.cloudfront.net/ivs/956482054022/cTo5UpKS07do/2020-07-13T22-54-42.188Z/OgRXMLtq8M11/media/hls/master.m3u8")
    val nonExistingStream = Sample("invalidLiveStreamSource", "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.DmumNckWFTqz_invalid.m3u8")
}
