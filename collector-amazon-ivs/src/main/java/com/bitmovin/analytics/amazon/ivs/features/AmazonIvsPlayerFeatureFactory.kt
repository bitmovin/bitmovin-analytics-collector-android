package com.bitmovin.analytics.amazon.ivs.features

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.license.FeatureConfigContainer

class AmazonIvsPlayerFeatureFactory(private val analytics: BitmovinAnalytics, private val player: Player) : FeatureFactory {
    override fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = mutableListOf<Feature<FeatureConfigContainer, *>>()
        // TODO "Not yet implemented"
        return features
    }
}
