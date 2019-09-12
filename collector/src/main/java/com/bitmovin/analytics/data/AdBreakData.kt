package com.bitmovin.analytics.data

data class AdBreakData(var adPosition: String?,
                       var adOffset: String?,
                       var adScheduleTime: Long?,
                       var adReplaceContentDuration: Long?,
                       var adPreloadOffset: Long?,
                       var adTagPath: String?,
                       var adTagServer: String?,
                       var adTagType: String?,
                       var adTagUrl: String?,
                       var adIsPersistent: Boolean?,
                       var adIdPlayer: String?,
                       var manifestDownloadTime: Long?,
                       var errorCode: Long?,
                       var errorData: String?,
                       var errorMessage: String?,
                       var adFallbackIndex: Long = 0) {
}