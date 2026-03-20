package com.bitmovin.analytics.theoplayer.ads

import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.ads.AdPosition
import com.bitmovin.analytics.theoplayer.TheoPlayerUtils
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.ads.ima.GoogleImaAdBreak
import com.theoplayer.android.api.ads.AdBreak as TheoAdBreak

internal object AdBreakMapper {
    fun fromTheoAdBreak(playerAdBreak: TheoAdBreak): AdBreak {
        val result =
            AdBreak(
                // For now we are using uuid since there seems to be property that can be used
                id = Util.uUID,
                ads = emptyList(),
            )

        try {
            if (TheoPlayerUtils.isTheoImaAdBreakClassLoaded && playerAdBreak is GoogleImaAdBreak) {
                val imaAdPodInfo = playerAdBreak.imaAdPodInfo

                if (imaAdPodInfo != null) {
                    result.scheduleTime = Util.secondsToMillis(imaAdPodInfo.timeOffset)
                    result.position = getPosition(playerAdBreak.timeOffset)

//                TODO: it seems like we cannot detect the tagType (vast vs vmap) same for tagUrl
//                TODO: Clarify: scheduleTime vs position vs offset? (also ad vs adBreak)
                }
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "on fromTheoAdBreak", e)
        }
        return result
    }

    private fun getPosition(timeOffset: Int): AdPosition =
        when {
            timeOffset == 0 -> AdPosition.PRE
            timeOffset < 0 -> AdPosition.POST
            else -> AdPosition.MID
        }

    private const val TAG = "AdBreakMapper"
}
