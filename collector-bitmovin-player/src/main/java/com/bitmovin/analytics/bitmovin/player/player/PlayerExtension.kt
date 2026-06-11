package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.media.video.quality.VideoQuality
import com.bitmovin.player.base.internal.plugin.Plugin
import com.bitmovin.player.core.internal.extensionPoint

/** Class to mark a [Player] as attached to a collector. */
private class CollectorPlayerPlugin : Plugin

/** Class to test if removePlugin function is supported. */
private class DummyAnalyticsCollectorPlugin : Plugin

/** Return the [Player]'s [ExtensionPoint] if this [Player] version supports it. */
internal val Player.maybeExtensionPoint get() =
    try {
        this.extensionPoint
    } catch (e: Throwable) {
        // we swallow the throwable, since ClassDefNotFoundError is expected on older player versions
        // because the extension point was introduced in v3.39.0
        null
    }

internal fun Player.attachCollector() =
    maybeExtensionPoint?.run {
        // We need to test if removing of plugin is supported with this player
        // version, since we could otherwise end up in a bug where attaching works on the
        // first run but not on the second run (since removal doesn't work)
        if (!supportsRemovePlugin()) {
            return Unit
        }

        if (this.getPlugin(CollectorPlayerPlugin::class) != null) {
            throw IllegalStateException("Collector already attached to player")
        }

        this.addPlugin(CollectorPlayerPlugin())
    }

/** Mark a [Player] as detached from any collector. */
internal fun Player.detachCollector() {
    maybeExtensionPoint?.runCatching {
        removePlugin(CollectorPlayerPlugin::class)
    }
}

internal fun Player.supportsRemovePlugin() =
    runCatching {
        extensionPoint.removePlugin(DummyAnalyticsCollectorPlugin::class)
    }.isSuccess

internal fun Player.extractVideoQualityInfo(): VideoQualityHolder? {
    val activeVideoQuality = this.playbackVideoData
    val manifestVideoBitrate = this.resolveManifestVideoBitrate(activeVideoQuality)

    // For adaptive sources (non-empty availableVideoQualities) a playbackVideoData whose id is not
    // in the current source's manifest does not belong to this source - it is stale data left over
    // from the previous source during a playlist transition. Reporting it would leak the previous
    // source's bitrate into the new source, so we treat the quality as not-yet-known and let the
    // source's VideoPlaybackQualityChanged event (or a manifest seed) populate it instead.
    if (manifestVideoBitrate == null && !this.source?.availableVideoQualities.isNullOrEmpty()) {
        return null
    }

    return VideoQualityHolder(activeVideoQuality, manifestVideoBitrate)
}

// Looks the currently playing quality up in [Source.availableVideoQualities] by its id to report
// the manifest bitrate, falling back to the quality's own bitrate when no manifest match exists
// (e.g. progressive streams or before the manifest qualities are available).
internal fun Player.resolveManifestVideoBitrate(videoQuality: VideoQuality?): Int? {
    videoQuality ?: return null
    val manifestQuality = this.source?.availableVideoQualities?.firstOrNull { it.id == videoQuality.id }
    return manifestQuality?.bitrate ?: videoQuality.bitrate
}
