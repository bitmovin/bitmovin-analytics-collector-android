package com.bitmovin.analytics.example.shared

import android.net.Uri

data class Sample(val name: String, val uri: Uri, val drmScheme: String? = null, val drmLicenseUri: Uri? = null) {
    constructor(name: String, uri: String, drmScheme: String? = null, drmLicenseUri: String? = null) :
            this(name, Uri.parse(uri), drmScheme, drmLicenseUri?.let { Uri.parse(it) })
}

// https://github.com/google/ExoPlayer/blob/release-v1/demo/src/main/java/com/google/android/exoplayer/demo/Samples.java
// https://github.com/google/ExoPlayer/blob/release-v2/demos/main/src/main/assets/media.exolist.json
object Samples {
    val HLS = Sample("Hls", "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8")
    val DASH = Sample("Dash", "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd")
    val SMOOTH = Sample("Smooth", "https://test.playready.microsoft.com/smoothstreaming/SSWSS720H264/SuperSpeedway_720.ism/manifest")
    val PROGRESSIVE = Sample("Progressive", "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/MI201109210084_mpeg-4_hd_high_1080p25_10mbits.mp4")

    val HLS_DRM_WIDEVINE = Sample("Hls + Widevine", "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/m3u8s/widevine-hls.m3u8", "widevine", "https://widevine-proxy.appspot.com/proxy")
    val HLS_DRM_WIDEVINE_ENCRYPTED = Sample("Encrypted Hls + Widevine", "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/m3u8s/11331.m3u8", "widevine", "https://widevine-proxy.appspot.com/proxy")
    val DASH_DRM_WIDEVINE = Sample("Dash + Widevine", "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd", "widevine", "https://widevine-proxy.appspot.com/proxy")
}