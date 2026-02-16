package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.player.api.Player

fun Player.useLowestRendition() {
    this.config.adaptationConfig.maxSelectableVideoBitrate = 1
}
