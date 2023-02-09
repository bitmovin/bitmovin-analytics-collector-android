package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.Quality

// This class stores the current quality since we need to
// have the old quality stored when a quality change event comes in
internal class PlaybackQualityProvider {
    var currentQuality: Quality? = null

    fun didQualityChange(newQuality: Quality): Boolean {
        return newQuality != currentQuality
    }
}
