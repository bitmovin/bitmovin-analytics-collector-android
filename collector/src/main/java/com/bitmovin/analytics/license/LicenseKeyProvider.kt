package com.bitmovin.analytics.license

import com.bitmovin.analytics.internal.InternalBitmovinApi
import com.bitmovin.analytics.license.LicenseKeyState.Provided
import com.bitmovin.analytics.license.LicenseKeyState.Result
import com.bitmovin.analytics.license.LicenseKeyState.Timeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private val DEFERRED_LICENSE_KEY_LOADING_TIMEOUT = 60.seconds

@InternalBitmovinApi
interface LicenseKeyProvider {
    val licenseKey: StateFlow<LicenseKeyState>
}

/**
 * A [LicenseKeyProvider] that provides the license key at a later time.
 */
@InternalBitmovinApi
class DeferredLicenseKeyProvider(
    override val licenseKey: StateFlow<LicenseKeyState>,
) : LicenseKeyProvider

/**
 * A [LicenseKeyProvider] that provides the license key immediately.
 */
@InternalBitmovinApi
class InstantLicenseKeyProvider(key: String) : LicenseKeyProvider {
    override val licenseKey: StateFlow<LicenseKeyState> = MutableStateFlow(Provided(key))
}

@InternalBitmovinApi
sealed interface LicenseKeyState {
    sealed interface Result : LicenseKeyState

    /**
     * The license key is not yet available and will be provided later.
     */
    object Deferred : LicenseKeyState

    /**
     * The license key was not provided in time.
     */
    object Timeout : Result

    /**
     * The license key was not provided.
     */
    object NotProvided : Result

    /**
     * The license key was provided - either by deferred license loading or directly.
     */
    data class Provided(val licenseKey: String) : Result
}

/**
 * Waits for a specified amount of time for the license key to be provided or a failure to occur.
 */
internal suspend fun LicenseKeyProvider.waitForResult(): Result = withTimeoutOrNull(
    DEFERRED_LICENSE_KEY_LOADING_TIMEOUT,
) { licenseKey.filterIsInstance<Result>().first() } ?: Timeout

internal val LicenseKeyProvider.licenseKeyOrNull: String?
    get() = when (val result = licenseKey.value) {
        is Provided -> result.licenseKey
        else -> null
    }
