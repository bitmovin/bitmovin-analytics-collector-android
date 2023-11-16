package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.license.LicenseKeyState
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventEmitter
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.data.AnalyticsLicenseData
import com.bitmovin.player.api.event.data.LicenseData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.reflect.KClass

private const val DEFERRED_LICENSE_KEY_PLACEHOLDER = "DEFERRED"

class DeferredLicenseRelayTest {
    @Test
    fun `getting the licenseKeyProvider returns a provider with immediate license key if deferred loading is not active`() {
        val key = "licenseKey"
        assertThat(DeferredLicenseRelay(key).licenseKeyProvider.licenseKey.value)
            .isEqualTo(LicenseKeyState.Provided(key))
    }

    @Test
    fun `getting the licenseKeyProvider returns a provider with deferred license key if deferred loading is active`() {
        assertThat(
            DeferredLicenseRelay(DEFERRED_LICENSE_KEY_PLACEHOLDER)
                .licenseKeyProvider
                .licenseKey
                .value,
        )
            .isEqualTo(LicenseKeyState.Deferred)
    }

    @Test
    fun `detaching a player from the relay unregisters the listener`() {
        val relay = DeferredLicenseRelay(DEFERRED_LICENSE_KEY_PLACEHOLDER)
        val player = TestEventEmitter()

        relay.attach(player)
        assertThat(player.listeners).hasSize(1)

        relay.detach()
        assertThat(player.listeners).hasSize(0)
    }

    @Test
    fun `receiving a LicenseValidated event unregisters the listener`() {
        val relay = DeferredLicenseRelay(licenseKey = DEFERRED_LICENSE_KEY_PLACEHOLDER)
        val player = TestEventEmitter()
        relay.attach(player)

        player.listeners.toList().forEach {
            it.invoke(
                PlayerEvent.LicenseValidated(
                    data = LicenseData(
                        AnalyticsLicenseData(key = "key"),
                    ),
                ),
            )
        }
        assertThat(player.listeners).hasSize(0)
    }

    @Test
    fun `receiving a LicenseValidated event with an analytics license key emits the key in the flow`() {
        val relay = DeferredLicenseRelay(licenseKey = DEFERRED_LICENSE_KEY_PLACEHOLDER)
        val player = TestEventEmitter()
        relay.attach(player)

        player.listeners.toList().forEach {
            it.invoke(
                PlayerEvent.LicenseValidated(
                    data = LicenseData(
                        AnalyticsLicenseData(key = "key"),
                    ),
                ),
            )
        }
        assertThat(relay.licenseKeyProvider.licenseKey.value)
            .isEqualTo(LicenseKeyState.Provided("key"))
    }

    @Test
    fun `receiving a LicenseValidated event without an analytics license key emits NotProvided in the flow`() {
        val relay = DeferredLicenseRelay(licenseKey = DEFERRED_LICENSE_KEY_PLACEHOLDER)
        val player = TestEventEmitter()
        relay.attach(player)

        player.listeners.toList().forEach {
            it.invoke(
                PlayerEvent.LicenseValidated(
                    data = LicenseData(
                        AnalyticsLicenseData(key = null),
                    ),
                ),
            )
        }
        assertThat(relay.licenseKeyProvider.licenseKey.value)
            .isEqualTo(LicenseKeyState.NotProvided)
    }
}

/**
 * An oversimplified event emitter test implementation that allows to
 * register and unregister listeners. Events are relayed to all registered listeners.
 */
@Suppress("UNCHECKED_CAST")
private class TestEventEmitter : EventEmitter<Event> {
    val listeners = mutableListOf<(Event) -> Unit>()
    override fun <E : Event> next(eventClass: KClass<E>, action: (E) -> Unit) {
        on(eventClass, action)
    }

    override fun <E : Event> off(action: (E) -> Unit) {
        listeners.remove(action as (Event) -> Unit)
    }

    override fun <E : Event> off(eventClass: KClass<E>, action: (E) -> Unit) {
        off(action)
    }

    override fun <E : Event> on(eventClass: KClass<E>, action: (E) -> Unit) {
        listeners.add(action as (Event) -> Unit)
    }
}
