package com.bitmovin.analytics.theoplayer.ads

import com.bitmovin.analytics.ads.Ad
import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.ads.LinearAd
import com.theoplayer.android.api.ads.Ad as TheoAd

/**
 * Extracts metadata for ads delivered through the "mediakind" custom server-side ad integration.
 *
 * MediaKind reports its ads as plain [LinearAd]s (so the duration is read off the linear ad) and
 * exposes all ad-specific metadata through [TheoAd.getCustomData], which for this integration is a
 * `Map<String, Any?>` rather than a dedicated ad type like [com.theoplayer.android.api.ads.ima.GoogleImaAd].
 *
 * Mirrors [AdMapper.extractMetadataFromGoogleImaAd]: it enriches the collector [Ad] in place so the
 * rest of the pipeline stays integration-agnostic.
 */
internal object MediaKindAdMapper {
    /** The value [TheoAd.getCustomIntegration] reports for ads coming from this integration. */
    const val INTEGRATION_ID = "mediakind"

    fun isMediaKindAd(theoAd: TheoAd): Boolean = theoAd.customIntegration?.equals(INTEGRATION_ID, ignoreCase = true) == true

    fun extractMetadata(
        ad: Ad,
        theoAd: TheoAd,
    ) {
        val customData = theoAd.customData.asStringMap()
        ad.adSystemName = customData[KEY_AD_SYSTEM]
        ad.creativeId = customData[KEY_CREATIVE_ID]
        ad.creativeAdId = customData[KEY_CREATIVE_AD_ID]
        ad.advertiserName = customData[KEY_ADVERTISER_NAME]
        ad.title = customData[KEY_TITLE]
        ad.universalAdIdValue = customData[KEY_UNIVERSAL_AD_ID_VALUE]
        ad.universalAdIdRegistry = customData[KEY_UNIVERSAL_AD_ID_REGISTRY]
        ad.isSlate = customData[KEY_IS_SLATE]?.toBoolean()

        // measurements
        if (theoAd is LinearAd) {
            ad.duration = Util.secondsToMillis(theoAd.durationAsDouble)
        }
    }

    /**
     * Reads [TheoAd.getCustomData] - typed as `Any?` since each integration decides its shape - as the
     * `Map<String, *>` MediaKind delivers. Entries with a non-string key or a null value are dropped,
     * values are stringified. Returns an empty map for any other (or absent) custom data shape.
     */
    private fun Any?.asStringMap(): Map<String, String> {
        val map = this as? Map<*, *> ?: return emptyMap()
        return map.entries
            .mapNotNull { (key, value) ->
                val stringKey = key as? String ?: return@mapNotNull null
                val stringValue = value?.toString() ?: return@mapNotNull null
                stringKey to stringValue
            }
            .toMap()
    }

    // Well-known keys mapped onto the collector Ad fields.
    private const val KEY_AD_SYSTEM = "adSystem"
    private const val KEY_CREATIVE_ID = "creativeId"
    private const val KEY_CREATIVE_AD_ID = "creativeAdId"
    private const val KEY_ADVERTISER_NAME = "advertiserName"
    private const val KEY_TITLE = "title"
    private const val KEY_UNIVERSAL_AD_ID_VALUE = "universalAdIdValue"
    private const val KEY_UNIVERSAL_AD_ID_REGISTRY = "universalAdIdRegistry"
    private const val KEY_IS_SLATE = "isSlate"
}
