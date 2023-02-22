package com.bitmovin.analytics.features.httprequesttracking

import androidx.annotation.Keep

@Keep // Protect from obfuscation in case customers are using proguard
enum class HttpRequestType {
    DRM_LICENSE_WIDEVINE,
    DRM_OTHER,
    MEDIA_THUMBNAILS,
    MEDIA_VIDEO,
    MEDIA_AUDIO,
    MEDIA_PROGRESSIVE,
    MEDIA_SUBTITLES,
    MANIFEST_DASH,
    MANIFEST_HLS,
    MANIFEST_HLS_MASTER,
    MANIFEST_HLS_VARIANT,
    MANIFEST_SMOOTH,
    MANIFEST,
    KEY_HLS_AES,
    UNKNOWN,
}
