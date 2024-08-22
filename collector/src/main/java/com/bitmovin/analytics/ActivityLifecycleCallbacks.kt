package com.bitmovin.analytics

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.bitmovin.analytics.ssai.SsaiService

/**
 * This class is used to listen to Activity Lifecycles and act upon it
 * Main purpose is to send out ssai adsamples that are collected, in case the app
 * goes into Pause
 *
 * Attention: This class is listening on all activity pauses of an APP, not only the one activity
 * where the player is currently used and thus very likely over reporting.
 * Since we only want to flush existing ssai ad samples, multiple calls
 * are not a problem. This class shouldn't be used to create new samples and send them out.
 */
internal class ActivityLifecycleCallbacks(private val ssaiService: SsaiService) : Application.ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity) {
        ssaiService.flushCurrentAdSample()
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        // do nothing
    }

    override fun onActivityStarted(activity: Activity) {
        // do nothing
    }

    override fun onActivityResumed(activity: Activity) {
        // do nothing
    }

    override fun onActivityStopped(activity: Activity) {
        // do nothing
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {
        // do nothing
    }

    override fun onActivityDestroyed(activity: Activity) {
        // do nothing
    }
}
