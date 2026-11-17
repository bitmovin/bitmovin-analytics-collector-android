package com.bitmovin.analytics.theoplayer.ads

import com.bitmovin.analytics.ads.Ad
import com.theoplayer.android.api.ads.Ad as TheoAd

/**
 * Extracts ad metadata that integrations expose through [TheoAd.getCustomData].
 *
 * Several integrations (e.g. the MediaKind connector or OptiView Ads) report their ads as plain
 * [com.theoplayer.android.api.ads.LinearAd]s and expose all ad-specific metadata through
 * [TheoAd.getCustomData] as a `Map<String, Any?>`, rather than through a dedicated ad type like
 * [com.theoplayer.android.api.ads.ima.GoogleImaAd].
 *
 * The mapping is not tied to a specific integration: whenever the custom data is a map, the
 * well-known keys below are picked up. Keys that are absent leave the corresponding field untouched.
 */
internal object CustomDataAdMapper {
    fun extractMetadata(
        ad: Ad,
        theoAd: TheoAd,
    ) {
        val customData = theoAd.customData.asStringMap()
        if (customData.isEmpty()) {
            return
        }

        customData[KEY_AD_SYSTEM]?.let { ad.adSystemName = it }
        customData[KEY_CREATIVE_ID]?.let { ad.creativeId = it }
        customData[KEY_CREATIVE_AD_ID]?.let { ad.creativeAdId = it }
        customData[KEY_ADVERTISER_NAME]?.let { ad.advertiserName = it }
        customData[KEY_TITLE]?.let { ad.title = it }
        customData[KEY_UNIVERSAL_AD_ID_VALUE]?.let { ad.universalAdIdValue = it }
        customData[KEY_UNIVERSAL_AD_ID_REGISTRY]?.let { ad.universalAdIdRegistry = it }
        customData[KEY_IS_SLATE]?.let { ad.isSlate = it.toBoolean() }
    }

    /**
     * Reads [TheoAd.getCustomData] - typed as `Any?` since each integration decides its shape - as a
     * `Map<String, *>`. Entries with a non-string key or a null value are dropped, values are
     * stringified. Returns an empty map for any other (or absent) custom data shape.
     */
    private fun Any?.asStringMap(): Map<String, String> {
        val map = this as? Map<*, *> ?: return emptyMap()
        return map.entries
            .mapNotNull { (key, value) ->
                val stringKey = key as? String ?: return@mapNotNull null
                val stringValue = value?.toString() ?: return@mapNotNull null
                stringKey to stringValue
            }.toMap()
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
