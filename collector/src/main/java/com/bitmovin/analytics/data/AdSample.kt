package com.bitmovin.analytics.data

import com.bitmovin.analytics.ads.Ad

data class AdSample(
    var adStartupTime: Long? = null,
    var clicked: Long = 0,
    var clickPosition: Long? = null,
    var closed: Long = 0,
    var closePosition: Long? = null,
    var completed: Long = 0,
    var midpoint: Long? = 0,
    var percentageInViewport: Int? = null,
    var quartile1: Long = 0,
    var quartile3: Long = 0,
    var skipped: Long = 0,
    var skipPosition: Long? = null,
    var started: Long = 0,
    var timeHovered: Long? = null,
    var timeInViewport: Long? = null,
    var timePlayed: Long? = null,
    var timeUntilHover: Long? = null,
    var adPodPosition: Int? = null,
    var exitPosition: Long? = null,
    var playPercentage: Int? = null,
    var skipPercentage: Int? = null,
    var clickPercentage: Int? = null,
    var closePercentage: Int? = null,
    var errorPosition: Long? = null,
    var errorPercentage: Int? = null,
    var timeToContent: Long? = null,
    var timeFromContent: Long? = null,
    var manifestDownloadTime: Long? = null,
    var errorCode: Int? = null,
    var errorData: String? = null,
    var errorMessage: String? = null,
    var ad: Ad = Ad()
)
