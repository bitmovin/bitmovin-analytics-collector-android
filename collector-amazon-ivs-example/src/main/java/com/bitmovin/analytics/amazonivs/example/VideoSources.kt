package com.bitmovin.analytics.amazonivs.example

import android.net.Uri

// https://github.com/aws-samples/amazon-ivs-player-android-sample
object VideoSources {
    // 1080p30
    val source1 = Uri.parse("https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.DmumNckWFTqz.m3u8")

    // Square Video
    val source2 = Uri.parse("https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.XFAcAcypUxQm.m3u8")
}
