package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.license.DeferredLicenseKeyProvider
import com.bitmovin.analytics.license.InstantLicenseKeyProvider
import com.bitmovin.analytics.license.LicenseKeyProvider
import com.bitmovin.analytics.license.LicenseKeyState
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventEmitter
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Providing this license key will defer the loading of the license key until the
 * [PlayerEvent.LicenseValidated] event is received.
 */
private const val DEFERRED_LICENSE_KEY_PLACEHOLDER = "DEFERRED"
internal class DeferredLicenseRelay(licenseKey: String) {
    private val deferredLoadingEnabled = licenseKey == DEFERRED_LICENSE_KEY_PLACEHOLDER
    private val licenseKeyFlow = MutableStateFlow<LicenseKeyState>(LicenseKeyState.Deferred)

    val licenseKeyProvider: LicenseKeyProvider = if (deferredLoadingEnabled) {
        DeferredLicenseKeyProvider(licenseKeyFlow)
    } else {
        InstantLicenseKeyProvider(licenseKey)
    }

    private var unsubscribe: (() -> Unit)? = null

    fun attach(eventEmitter: EventEmitter<Event>) {
        if (!deferredLoadingEnabled) return
        eventEmitter.on(::onPlayerLicenseValidated)
        unsubscribe = { eventEmitter.off(::onPlayerLicenseValidated) }
    }

    fun detach() {
        if (!deferredLoadingEnabled) return
        detachInternally()
    }

    private fun onPlayerLicenseValidated(event: PlayerEvent.LicenseValidated) {
        detachInternally()
        val licenseKey = event.data.analytics.key
        val state = if (licenseKey != null) {
            LicenseKeyState.Provided(licenseKey)
        } else {
            LicenseKeyState.NotProvided
        }
        licenseKeyFlow.update { state }
    }

    private fun detachInternally() {
        unsubscribe?.invoke()
        unsubscribe = null
    }
}
