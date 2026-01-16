package com.bitmovin.analytics.theoplayer

import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.track.mediatrack.video.list.VideoTrackListEventTypes
import com.theoplayer.android.api.player.Player

fun Player.useLowestRendition() {
    this.videoTracks.addEventListener(
        VideoTrackListEventTypes.ADDTRACK,
        EventListener {
            this.videoTracks.getItem(0).targetQuality =
                this.videoTracks.getItem(0).getQualities().minBy { quality -> quality.bandwidth }
        },
    )
}

fun Player.useHighestRendition() {
    this.videoTracks.addEventListener(
        VideoTrackListEventTypes.ADDTRACK,
        EventListener {
            this.videoTracks.getItem(0).targetQuality =
                this.videoTracks.getItem(0).getQualities().maxBy { quality -> quality.bandwidth }
        },
    )
}
