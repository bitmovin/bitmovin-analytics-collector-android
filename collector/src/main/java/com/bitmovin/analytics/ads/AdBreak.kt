package com.bitmovin.analytics.ads

data class AdBreak(var id: String, var scheduleTime: Long, var ads: List<Ad>,
                   var adPosition: String? = null,
                   var adOffset: String? = null,
                   var adScheduleTime: Long? = null,
                   var adReplaceContentDuration: Long? = null,
                   var adPreloadOffset: Long? = null,
                   var adTagPath: String? = null,
                   var adTagServer: String? = null,
                   var adTagType: String? = null,
                   var adTagUrl: String? = null,
                   var adIsPersistent: Boolean? = null,
                   var adIdPlayer: String? = null,
                   var manifestDownloadTime: Long? = null,
                   var errorCode: Int? = null,
                   var errorData: String? = null,
                   var errorMessage: String? = null,
                   var adFallbackIndex: Long = 0) : AdConfig()