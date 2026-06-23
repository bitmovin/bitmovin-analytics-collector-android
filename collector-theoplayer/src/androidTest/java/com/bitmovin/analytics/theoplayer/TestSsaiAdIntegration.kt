package com.bitmovin.analytics.theoplayer

import com.theoplayer.android.api.ads.AdBreakInit
import com.theoplayer.android.api.ads.AdInit
import com.theoplayer.android.api.ads.ServerSideAdIntegrationController
import com.theoplayer.android.api.ads.ServerSideAdIntegrationFactory
import com.theoplayer.android.api.ads.ServerSideAdIntegrationHandler
import com.theoplayer.android.api.error.ErrorCode
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.ssai.CustomSsaiDescription
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionRegistry
import com.theoplayer.android.api.source.ssai.CustomSsaiDescriptionSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * A minimal custom server-side ad integration for system tests, modelled on THEOplayer's own
 * server-side ad integration API (the same one the Uplynk connector uses).
 *
 * It resolves the marked source to a plain playable stream (so content plays) and lets the test
 * inject ad breaks/pods deterministically through the [ServerSideAdIntegrationController] - far
 * faster and more reliable than waiting for a real Google DAI ad to play out. Ads driven through
 * the controller are reported by the player with integration [com.theoplayer.android.api.event.ads.AdIntegrationKind.CUSTOM],
 * which the collector routes to its SSAI pipeline (see TheoPlayerUtils.isClientSideAd).
 *
 * Usage from a test:
 * ```
 * val integration = TestSsaiAdIntegration(mainScope)
 * integration.register(player, TestSources.HLS_REDBULL.m3u8Url!!, SourceType.HLS)
 * // ... wait until content plays ...
 * integration.playAdBreak(timeOffset = 0, adCount = 1)
 * ```
 */
class TestSsaiAdIntegration(
    private val mainScope: CoroutineScope,
    val integrationId: String = INTEGRATION_ID,
) {
    private var controller: ServerSideAdIntegrationController? = null

    /** Whether the integration has been built by the player (i.e. the marked source was processed). */
    val isReady: Boolean
        get() = controller != null

    /**
     * Registers this integration on the given player and loads a source marked for it. THEOplayer
     * routes the marked source to [TestSsaiAdIntegrationHandler.setSource], which resolves it to the
     * plain [contentUrl] so that regular content plays back.
     *
     * Must be called from the main thread - this method switches to [mainScope] internally.
     */
    suspend fun register(
        player: Player,
        contentUrl: String,
        contentType: SourceType,
    ) {
        withContext(mainScope.coroutineContext) {
            CustomSsaiDescriptionRegistry.register(integrationId, TestCustomSsaiDescriptionSerializer(integrationId))

            val factory =
                object : ServerSideAdIntegrationFactory {
                    override fun build(controller: ServerSideAdIntegrationController): ServerSideAdIntegrationHandler {
                        this@TestSsaiAdIntegration.controller = controller
                        return TestSsaiAdIntegrationHandler(contentUrl, contentType)
                    }
                }
            player.ads.registerServerSideIntegration(integrationId, factory)

            val markedSource =
                TypedSource.Builder(contentUrl)
                    .type(contentType)
                    .ssai(TestCustomSsaiConfiguration(integrationId))
                    .build()
            player.source = SourceDescription.Builder(markedSource).build()
        }
    }

    /**
     * Begins a single-ad server-side break and leaves it in progress (does not end it). Useful for
     * abandonment scenarios where the player is destroyed while an ad is still playing.
     */
    suspend fun beginOpenAdBreak(timeOffset: Int) {
        val controller = this.controller ?: return
        onMain {
            val adBreak = controller.createAdBreak(AdBreakInit(timeOffset = timeOffset))
            val ad =
                controller.createAd(
                    AdInit(type = "linear", id = "test-ssai-open-$timeOffset", duration = 5),
                    adBreak,
                )
            controller.beginAd(ad)
            controller.updateAdProgress(ad, 0.25)
        }
    }

    /**
     * Drives a complete ad break through the controller: for each ad begin -> quartiles -> end.
     *
     * All ads are created up front so the break stays a single pod: ending one ad must not end the
     * break while later ads are still pending (otherwise each ad becomes its own break and the pod
     * position resets to 0).
     *
     * The controller calls run on the main thread, while the [stepDelayMs] spacing is awaited off
     * the main thread so the player keeps playing the underlying content in between.
     */
    suspend fun playAdBreak(
        timeOffset: Int,
        adCount: Int,
        stepDelayMs: Long = 500,
        durationInSeconds: Int = 5,
    ) {
        val controller = this.controller ?: return

        val adBreak = onMain { controller.createAdBreak(AdBreakInit(timeOffset = timeOffset)) }
        val ads =
            onMain {
                (0 until adCount).map { index ->
                    controller.createAd(
                        AdInit(type = "linear", id = "test-ssai-$timeOffset-$index", duration = durationInSeconds),
                        adBreak,
                    )
                }
            }

        for (ad in ads) {
            onMain { controller.beginAd(ad) }
            delay(stepDelayMs)
            onMain { controller.updateAdProgress(ad, 0.25) }
            delay(stepDelayMs)
            onMain { controller.updateAdProgress(ad, 0.5) }
            delay(stepDelayMs)
            onMain { controller.updateAdProgress(ad, 0.75) }
            delay(stepDelayMs)
            onMain { controller.endAd(ad) }
        }

        delay(stepDelayMs)
        onMain { controller.removeAdBreak(adBreak) }
    }

    /**
     * Begins a single-ad server-side break, plays into the ad, then raises a fatal error through the
     * controller while the ad is still in progress. [ServerSideAdIntegrationController.fatalError]
     * surfaces as a player error event which the collector routes through its state machine - and,
     * because an SSAI ad is running, this produces an SSAI ad error sample. The ad/break are left in
     * progress (the fatal error ends playback).
     */
    suspend fun playAdBreakWithFatalError(
        timeOffset: Int,
        error: Throwable = RuntimeException("test-ssai fatal ad error"),
        errorCode: ErrorCode = ErrorCode.AD_ERROR,
        stepDelayMs: Long = 500,
    ) {
        val controller = this.controller ?: return

        val adBreak = onMain { controller.createAdBreak(AdBreakInit(timeOffset = timeOffset)) }
        val ad =
            onMain {
                controller.createAd(
                    AdInit(type = "linear", id = "test-ssai-error-$timeOffset", duration = 5),
                    adBreak,
                )
            }

        onMain { controller.beginAd(ad) }
        delay(stepDelayMs)
        onMain { controller.updateAdProgress(ad, 0.25) }
        delay(stepDelayMs)
        onMain { controller.fatalError(error, errorCode) }
    }

    /**
     * Drives a single-ad break that hits a non-fatal error mid-ad - the kind a server-side
     * integration would raise when ad beaconing/tracking fails (e.g. it cannot parse the ad
     * metadata it received). [ServerSideAdIntegrationController.error] fires an `AD_ERROR` event but,
     * unlike [ServerSideAdIntegrationController.fatalError], does not stop playback: the ad plays out
     * and content continues afterwards. This lets tests assert that such an error stays scoped to the
     * ad and leaves the main content impression untouched.
     */
    suspend fun playAdBreakWithAdError(
        timeOffset: Int,
        error: Throwable = RuntimeException("test-ssai ad beaconing error"),
        stepDelayMs: Long = 500,
    ) {
        val controller = this.controller ?: return

        val adBreak = onMain { controller.createAdBreak(AdBreakInit(timeOffset = timeOffset)) }
        val ad =
            onMain {
                controller.createAd(
                    AdInit(type = "linear", id = "test-ssai-aderror-$timeOffset", duration = 5),
                    adBreak,
                )
            }

        onMain { controller.beginAd(ad) }
        delay(stepDelayMs)
        onMain { controller.updateAdProgress(ad, 0.25) }
        delay(stepDelayMs)
        // non-fatal: fires AD_ERROR but playback continues, so the ad still plays to completion
        onMain { controller.error(error) }
        delay(stepDelayMs)
        onMain { controller.updateAdProgress(ad, 0.5) }
        delay(stepDelayMs)
        onMain { controller.updateAdProgress(ad, 0.75) }
        delay(stepDelayMs)
        onMain { controller.endAd(ad) }

        delay(stepDelayMs)
        onMain { controller.removeAdBreak(adBreak) }
    }

    private suspend fun <T> onMain(block: () -> T): T = withContext(mainScope.coroutineContext) { block() }

    /**
     * Handler that transforms the marked source into a plain, playable source. Returning a source
     * without an SSAI marker terminates resolution (the player just plays it).
     */
    private class TestSsaiAdIntegrationHandler(
        private val contentUrl: String,
        private val contentType: SourceType,
    ) : ServerSideAdIntegrationHandler {
        override suspend fun setSource(source: SourceDescription): SourceDescription {
            val resolvedSource = TypedSource.Builder(contentUrl).type(contentType).build()
            return SourceDescription.Builder(resolvedSource).build()
        }
    }

    /** SSAI source configuration that routes a source to this test custom integration. */
    private class TestCustomSsaiConfiguration(
        private val integrationId: String,
    ) : CustomSsaiDescription() {
        override val customIntegration: String
            get() = integrationId
    }

    /**
     * Minimal serializer required by [CustomSsaiDescriptionRegistry]. Our configuration only carries
     * the integration id, so the JSON representation is just that id.
     */
    private class TestCustomSsaiDescriptionSerializer(
        private val integrationId: String,
    ) : CustomSsaiDescriptionSerializer {
        override fun toJson(value: CustomSsaiDescription): String = "{\"integration\":\"$integrationId\"}"

        override fun fromJson(json: String): CustomSsaiDescription = TestCustomSsaiConfiguration(integrationId)
    }

    companion object {
        const val INTEGRATION_ID = "test-custom-ssai"
    }
}
