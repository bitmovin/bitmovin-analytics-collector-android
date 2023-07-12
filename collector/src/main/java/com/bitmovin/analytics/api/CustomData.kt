package com.bitmovin.analytics.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Free-form data that can be used to enrich the analytics data
 */
@Parcelize
data class CustomData(
    /**
     * Optional free-form data
     */
    val customData1: String? = null,
    /**
     * Optional free-form data
     */
    val customData2: String? = null,
    /**
     * Optional free-form data
     */
    val customData3: String? = null,
    /**
     * Optional free-form data
     */
    val customData4: String? = null,
    /**
     * Optional free-form data
     */
    val customData5: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData6: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData7: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData8: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData9: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData10: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData11: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData12: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData13: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData14: String? = null,
    /**
     * Optional free-form data. Not enabled by default Must be activated for your organization
     */
    val customData15: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData16: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData17: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData18: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData19: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData20: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData21: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData22: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData23: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData24: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData25: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData26: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData27: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData28: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData29: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData30: String? = null,
    /**
     * Free form-data field that can be used for A/B testing
     */
    val experimentName: String? = null,
) : Parcelable
