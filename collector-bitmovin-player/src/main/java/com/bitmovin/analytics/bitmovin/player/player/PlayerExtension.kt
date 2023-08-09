package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.player.api.Player
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

internal fun Player.attachCollector() = maybeExtensionPoint?.run {
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

internal fun Player.supportsRemovePlugin() = runCatching {
    extensionPoint.removePlugin(DummyAnalyticsCollectorPlugin::class)
}.isSuccess
