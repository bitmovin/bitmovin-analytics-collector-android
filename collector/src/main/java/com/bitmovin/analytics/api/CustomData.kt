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
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData31: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData32: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData33: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData34: String? = null,
    /**
     * Optional free-form data. Not enabled by default Must be activated for your organization
     */
    val customData35: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData36: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData37: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData38: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData39: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData40: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData41: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData42: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData43: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData44: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData45: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData46: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData47: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData48: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData49: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData50: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData51: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData52: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData53: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData54: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData55: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData56: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData57: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData58: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData59: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData60: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData61: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData62: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData63: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData64: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData65: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData66: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData67: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData68: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData69: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData70: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData71: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData72: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData73: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData74: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData75: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData76: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData77: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData78: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData79: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData80: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData81: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData82: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData83: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData84: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData85: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData86: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData87: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData88: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData89: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData90: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData91: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData92: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData93: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData94: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData95: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData96: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData97: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData98: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData99: String? = null,
    /**
     * Optional free-form data. Not enabled by default. Must be activated for your organization
     */
    val customData100: String? = null,
) : Parcelable {
    constructor(
        /**
         * Optional free-form data
         */
        customData1: String? = null,
        /**
         * Optional free-form data
         */
        customData2: String? = null,
        /**
         * Optional free-form data
         */
        customData3: String? = null,
        /**
         * Optional free-form data
         */
        customData4: String? = null,
        /**
         * Optional free-form data
         */
        customData5: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData6: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData7: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData8: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData9: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData10: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData11: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData12: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData13: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData14: String? = null,
        /**
         * Optional free-form data. Not enabled by default Must be activated for your organization
         */
        customData15: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData16: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData17: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData18: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData19: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData20: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData21: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData22: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData23: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData24: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData25: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData26: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData27: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData28: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData29: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData30: String? = null,
        /**
         * Free form-data field that can be used for A/B testing
         */
        experimentName: String? = null,
    ) : this (
        customData1 = customData1, customData2 = customData2, customData3 = customData3, customData4 = customData4,
        customData5 = customData5, customData6 = customData6, customData7 = customData7, customData8 = customData8,
        customData9 = customData9, customData10 = customData10, customData11 = customData11, customData12 = customData12,
        customData13 = customData13, customData14 = customData14, customData15 = customData15, customData16 = customData16,
        customData17 = customData17, customData18 = customData18, customData19 = customData19, customData20 = customData20,
        customData21 = customData21, customData22 = customData22, customData23 = customData23, customData24 = customData24,
        customData25 = customData25, customData26 = customData26, customData27 = customData27, customData28 = customData28,
        customData29 = customData29, customData30 = customData30, experimentName = experimentName,
        customData31 = null, customData61 = null,
    )

    constructor(
        /**
         * Optional free-form data
         */
        customData1: String? = null,
        /**
         * Optional free-form data
         */
        customData2: String? = null,
        /**
         * Optional free-form data
         */
        customData3: String? = null,
        /**
         * Optional free-form data
         */
        customData4: String? = null,
        /**
         * Optional free-form data
         */
        customData5: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData6: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData7: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData8: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData9: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData10: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData11: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData12: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData13: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData14: String? = null,
        /**
         * Optional free-form data. Not enabled by default Must be activated for your organization
         */
        customData15: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData16: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData17: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData18: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData19: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData20: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData21: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData22: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData23: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData24: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData25: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData26: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData27: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData28: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData29: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData30: String? = null,
        /**
         * Free form-data field that can be used for A/B testing
         */
        experimentName: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData31: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData32: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData33: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData34: String? = null,
        /**
         * Optional free-form data. Not enabled by default Must be activated for your organization
         */
        customData35: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData36: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData37: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData38: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData39: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData40: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData41: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData42: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData43: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData44: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData45: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData46: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData47: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData48: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData49: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData50: String? = null,
    ) : this (
        customData1 = customData1, customData2 = customData2, customData3 = customData3, customData4 = customData4,
        customData5 = customData5, customData6 = customData6, customData7 = customData7, customData8 = customData8,
        customData9 = customData9, customData10 = customData10, customData11 = customData11, customData12 = customData12,
        customData13 = customData13, customData14 = customData14, customData15 = customData15, customData16 = customData16,
        customData17 = customData17, customData18 = customData18, customData19 = customData19, customData20 = customData20,
        customData21 = customData21, customData22 = customData22, customData23 = customData23, customData24 = customData24,
        customData25 = customData25, customData26 = customData26, customData27 = customData27, customData28 = customData28,
        customData29 = customData29, customData30 = customData30, experimentName = experimentName, customData31 = customData31,
        customData32 = customData32,
        customData33 = customData33, customData34 = customData34, customData35 = customData35, customData36 = customData36,
        customData37 = customData37, customData38 = customData38, customData39 = customData39, customData40 = customData40,
        customData41 = customData41, customData42 = customData42, customData43 = customData43, customData44 = customData44,
        customData45 = customData45, customData46 = customData46, customData47 = customData47, customData48 = customData48,
        customData49 = customData49, customData50 = customData50, customData51 = null, customData61 = null,
    )

    constructor(
        /**
         * Optional free-form data
         */
        customData1: String? = null,
        /**
         * Optional free-form data
         */
        customData2: String? = null,
        /**
         * Optional free-form data
         */
        customData3: String? = null,
        /**
         * Optional free-form data
         */
        customData4: String? = null,
        /**
         * Optional free-form data
         */
        customData5: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData6: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData7: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData8: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData9: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData10: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData11: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData12: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData13: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData14: String? = null,
        /**
         * Optional free-form data. Not enabled by default Must be activated for your organization
         */
        customData15: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData16: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData17: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData18: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData19: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData20: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData21: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData22: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData23: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData24: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData25: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData26: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData27: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData28: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData29: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData30: String? = null,
        /**
         * Free form-data field that can be used for A/B testing
         */
        experimentName: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData31: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData32: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData33: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData34: String? = null,
        /**
         * Optional free-form data. Not enabled by default Must be activated for your organization
         */
        customData35: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData36: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData37: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData38: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData39: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData40: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData41: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData42: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData43: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData44: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData45: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData46: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData47: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData48: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData49: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData50: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData51: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData52: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData53: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData54: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData55: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData56: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData57: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData58: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData59: String? = null,
        /**
         * Optional free-form data. Not enabled by default. Must be activated for your organization
         */
        customData60: String? = null,
    ) : this (
        customData1 = customData1, customData2 = customData2, customData3 = customData3, customData4 = customData4,
        customData5 = customData5, customData6 = customData6, customData7 = customData7, customData8 = customData8,
        customData9 = customData9, customData10 = customData10, customData11 = customData11, customData12 = customData12,
        customData13 = customData13, customData14 = customData14, customData15 = customData15, customData16 = customData16,
        customData17 = customData17, customData18 = customData18, customData19 = customData19, customData20 = customData20,
        customData21 = customData21, customData22 = customData22, customData23 = customData23, customData24 = customData24,
        customData25 = customData25, customData26 = customData26, customData27 = customData27, customData28 = customData28,
        customData29 = customData29, customData30 = customData30, experimentName = experimentName, customData31 = customData31,
        customData32 = customData32,
        customData33 = customData33, customData34 = customData34, customData35 = customData35, customData36 = customData36,
        customData37 = customData37, customData38 = customData38, customData39 = customData39, customData40 = customData40,
        customData41 = customData41, customData42 = customData42, customData43 = customData43, customData44 = customData44,
        customData45 = customData45, customData46 = customData46, customData47 = customData47, customData48 = customData48,
        customData49 = customData49, customData50 = customData50, customData51 = customData51, customData52 = customData52,
        customData53 = customData53, customData54 = customData54, customData55 = customData55, customData56 = customData56,
        customData57 = customData57, customData58 = customData58, customData59 = customData59, customData60 = customData60,
        customData61 = null,
    )

    fun buildUpon(): Builder {
        return Builder(this)
    }

    class Builder {
        constructor() : this(customData = CustomData())

        constructor(customData: CustomData) {
            this.customData1 = customData.customData1
            this.customData2 = customData.customData2
            this.customData3 = customData.customData3
            this.customData4 = customData.customData4
            this.customData5 = customData.customData5
            this.customData6 = customData.customData6
            this.customData7 = customData.customData7
            this.customData8 = customData.customData8
            this.customData9 = customData.customData9
            this.customData10 = customData.customData10
            this.customData11 = customData.customData11
            this.customData12 = customData.customData12
            this.customData13 = customData.customData13
            this.customData14 = customData.customData14
            this.customData15 = customData.customData15
            this.customData16 = customData.customData16
            this.customData17 = customData.customData17
            this.customData18 = customData.customData18
            this.customData19 = customData.customData19
            this.customData20 = customData.customData20
            this.customData21 = customData.customData21
            this.customData22 = customData.customData22
            this.customData23 = customData.customData23
            this.customData24 = customData.customData24
            this.customData25 = customData.customData25
            this.customData26 = customData.customData26
            this.customData27 = customData.customData27
            this.customData28 = customData.customData28
            this.customData29 = customData.customData29
            this.customData30 = customData.customData30
            this.experimentName = customData.experimentName
            this.customData31 = customData.customData31
            this.customData32 = customData.customData32
            this.customData33 = customData.customData33
            this.customData34 = customData.customData34
            this.customData35 = customData.customData35
            this.customData36 = customData.customData36
            this.customData37 = customData.customData37
            this.customData38 = customData.customData38
            this.customData39 = customData.customData39
            this.customData40 = customData.customData40
            this.customData41 = customData.customData41
            this.customData42 = customData.customData42
            this.customData43 = customData.customData43
            this.customData44 = customData.customData44
            this.customData45 = customData.customData45
            this.customData46 = customData.customData46
            this.customData47 = customData.customData47
            this.customData48 = customData.customData48
            this.customData49 = customData.customData49
            this.customData50 = customData.customData50
            this.customData51 = customData.customData51
            this.customData52 = customData.customData52
            this.customData53 = customData.customData53
            this.customData54 = customData.customData54
            this.customData55 = customData.customData55
            this.customData56 = customData.customData56
            this.customData57 = customData.customData57
            this.customData58 = customData.customData58
            this.customData59 = customData.customData59
            this.customData60 = customData.customData60
            this.customData61 = customData.customData61
            this.customData62 = customData.customData62
            this.customData63 = customData.customData63
            this.customData64 = customData.customData64
            this.customData65 = customData.customData65
            this.customData66 = customData.customData66
            this.customData67 = customData.customData67
            this.customData68 = customData.customData68
            this.customData69 = customData.customData69
            this.customData70 = customData.customData70
            this.customData71 = customData.customData71
            this.customData72 = customData.customData72
            this.customData73 = customData.customData73
            this.customData74 = customData.customData74
            this.customData75 = customData.customData75
            this.customData76 = customData.customData76
            this.customData77 = customData.customData77
            this.customData78 = customData.customData78
            this.customData79 = customData.customData79
            this.customData80 = customData.customData80
            this.customData81 = customData.customData81
            this.customData82 = customData.customData82
            this.customData83 = customData.customData83
            this.customData84 = customData.customData84
            this.customData85 = customData.customData85
            this.customData86 = customData.customData86
            this.customData87 = customData.customData87
            this.customData88 = customData.customData88
            this.customData89 = customData.customData89
            this.customData90 = customData.customData90
            this.customData91 = customData.customData91
            this.customData92 = customData.customData92
            this.customData93 = customData.customData93
            this.customData94 = customData.customData94
            this.customData95 = customData.customData95
            this.customData96 = customData.customData96
            this.customData97 = customData.customData97
            this.customData98 = customData.customData98
            this.customData99 = customData.customData99
            this.customData100 = customData.customData100
        }

        private var customData1: String? = null
        private var customData2: String? = null
        private var customData3: String? = null
        private var customData4: String? = null
        private var customData5: String? = null
        private var customData6: String? = null
        private var customData7: String? = null
        private var customData8: String? = null
        private var customData9: String? = null
        private var customData10: String? = null
        private var customData11: String? = null
        private var customData12: String? = null
        private var customData13: String? = null
        private var customData14: String? = null
        private var customData15: String? = null
        private var customData16: String? = null
        private var customData17: String? = null
        private var customData18: String? = null
        private var customData19: String? = null
        private var customData20: String? = null
        private var customData21: String? = null
        private var customData22: String? = null
        private var customData23: String? = null
        private var customData24: String? = null
        private var customData25: String? = null
        private var customData26: String? = null
        private var customData27: String? = null
        private var customData28: String? = null
        private var customData29: String? = null
        private var customData30: String? = null
        private var experimentName: String? = null
        private var customData31: String? = null
        private var customData32: String? = null
        private var customData33: String? = null
        private var customData34: String? = null
        private var customData35: String? = null
        private var customData36: String? = null
        private var customData37: String? = null
        private var customData38: String? = null
        private var customData39: String? = null
        private var customData40: String? = null
        private var customData41: String? = null
        private var customData42: String? = null
        private var customData43: String? = null
        private var customData44: String? = null
        private var customData45: String? = null
        private var customData46: String? = null
        private var customData47: String? = null
        private var customData48: String? = null
        private var customData49: String? = null
        private var customData50: String? = null
        private var customData51: String? = null
        private var customData52: String? = null
        private var customData53: String? = null
        private var customData54: String? = null
        private var customData55: String? = null
        private var customData56: String? = null
        private var customData57: String? = null
        private var customData58: String? = null
        private var customData59: String? = null
        private var customData60: String? = null
        private var customData61: String? = null
        private var customData62: String? = null
        private var customData63: String? = null
        private var customData64: String? = null
        private var customData65: String? = null
        private var customData66: String? = null
        private var customData67: String? = null
        private var customData68: String? = null
        private var customData69: String? = null
        private var customData70: String? = null
        private var customData71: String? = null
        private var customData72: String? = null
        private var customData73: String? = null
        private var customData74: String? = null
        private var customData75: String? = null
        private var customData76: String? = null
        private var customData77: String? = null
        private var customData78: String? = null
        private var customData79: String? = null
        private var customData80: String? = null
        private var customData81: String? = null
        private var customData82: String? = null
        private var customData83: String? = null
        private var customData84: String? = null
        private var customData85: String? = null
        private var customData86: String? = null
        private var customData87: String? = null
        private var customData88: String? = null
        private var customData89: String? = null
        private var customData90: String? = null
        private var customData91: String? = null
        private var customData92: String? = null
        private var customData93: String? = null
        private var customData94: String? = null
        private var customData95: String? = null
        private var customData96: String? = null
        private var customData97: String? = null
        private var customData98: String? = null
        private var customData99: String? = null
        private var customData100: String? = null

        fun setCustomData1(customData1: String?) = apply { this.customData1 = customData1 }

        fun setCustomData2(customData2: String?) = apply { this.customData2 = customData2 }

        fun setCustomData3(customData3: String?) = apply { this.customData3 = customData3 }

        fun setCustomData4(customData4: String?) = apply { this.customData4 = customData4 }

        fun setCustomData5(customData5: String?) = apply { this.customData5 = customData5 }

        fun setCustomData6(customData6: String?) = apply { this.customData6 = customData6 }

        fun setCustomData7(customData7: String?) = apply { this.customData7 = customData7 }

        fun setCustomData8(customData8: String?) = apply { this.customData8 = customData8 }

        fun setCustomData9(customData9: String?) = apply { this.customData9 = customData9 }

        fun setCustomData10(customData10: String?) = apply { this.customData10 = customData10 }

        fun setCustomData11(customData11: String?) = apply { this.customData11 = customData11 }

        fun setCustomData12(customData12: String?) = apply { this.customData12 = customData12 }

        fun setCustomData13(customData13: String?) = apply { this.customData13 = customData13 }

        fun setCustomData14(customData14: String?) = apply { this.customData14 = customData14 }

        fun setCustomData15(customData15: String?) = apply { this.customData15 = customData15 }

        fun setCustomData16(customData16: String?) = apply { this.customData16 = customData16 }

        fun setCustomData17(customData17: String?) = apply { this.customData17 = customData17 }

        fun setCustomData18(customData18: String?) = apply { this.customData18 = customData18 }

        fun setCustomData19(customData19: String?) = apply { this.customData19 = customData19 }

        fun setCustomData20(customData20: String?) = apply { this.customData20 = customData20 }

        fun setCustomData21(customData21: String?) = apply { this.customData21 = customData21 }

        fun setCustomData22(customData22: String?) = apply { this.customData22 = customData22 }

        fun setCustomData23(customData23: String?) = apply { this.customData23 = customData23 }

        fun setCustomData24(customData24: String?) = apply { this.customData24 = customData24 }

        fun setCustomData25(customData25: String?) = apply { this.customData25 = customData25 }

        fun setCustomData26(customData26: String?) = apply { this.customData26 = customData26 }

        fun setCustomData27(customData27: String?) = apply { this.customData27 = customData27 }

        fun setCustomData28(customData28: String?) = apply { this.customData28 = customData28 }

        fun setCustomData29(customData29: String?) = apply { this.customData29 = customData29 }

        fun setCustomData30(customData30: String?) = apply { this.customData30 = customData30 }

        fun setExperimentName(experimentName: String?) = apply { this.experimentName = experimentName }

        fun setCustomData31(customData31: String?) = apply { this.customData31 = customData31 }

        fun setCustomData32(customData32: String?) = apply { this.customData32 = customData32 }

        fun setCustomData33(customData33: String?) = apply { this.customData33 = customData33 }

        fun setCustomData34(customData34: String?) = apply { this.customData34 = customData34 }

        fun setCustomData35(customData35: String?) = apply { this.customData35 = customData35 }

        fun setCustomData36(customData36: String?) = apply { this.customData36 = customData36 }

        fun setCustomData37(customData37: String?) = apply { this.customData37 = customData37 }

        fun setCustomData38(customData38: String?) = apply { this.customData38 = customData38 }

        fun setCustomData39(customData39: String?) = apply { this.customData39 = customData39 }

        fun setCustomData40(customData40: String?) = apply { this.customData40 = customData40 }

        fun setCustomData41(customData41: String?) = apply { this.customData41 = customData41 }

        fun setCustomData42(customData42: String?) = apply { this.customData42 = customData42 }

        fun setCustomData43(customData43: String?) = apply { this.customData43 = customData43 }

        fun setCustomData44(customData44: String?) = apply { this.customData44 = customData44 }

        fun setCustomData45(customData45: String?) = apply { this.customData45 = customData45 }

        fun setCustomData46(customData46: String?) = apply { this.customData46 = customData46 }

        fun setCustomData47(customData47: String?) = apply { this.customData47 = customData47 }

        fun setCustomData48(customData48: String?) = apply { this.customData48 = customData48 }

        fun setCustomData49(customData49: String?) = apply { this.customData49 = customData49 }

        fun setCustomData50(customData50: String?) = apply { this.customData50 = customData50 }

        fun setCustomData51(customData51: String?) = apply { this.customData51 = customData51 }

        fun setCustomData52(customData52: String?) = apply { this.customData52 = customData52 }

        fun setCustomData53(customData53: String?) = apply { this.customData53 = customData53 }

        fun setCustomData54(customData54: String?) = apply { this.customData54 = customData54 }

        fun setCustomData55(customData55: String?) = apply { this.customData55 = customData55 }

        fun setCustomData56(customData56: String?) = apply { this.customData56 = customData56 }

        fun setCustomData57(customData57: String?) = apply { this.customData57 = customData57 }

        fun setCustomData58(customData58: String?) = apply { this.customData58 = customData58 }

        fun setCustomData59(customData59: String?) = apply { this.customData59 = customData59 }

        fun setCustomData60(customData60: String?) = apply { this.customData60 = customData60 }

        fun setCustomData61(customData61: String?) = apply { this.customData61 = customData61 }

        fun setCustomData62(customData62: String?) = apply { this.customData62 = customData62 }

        fun setCustomData63(customData63: String?) = apply { this.customData63 = customData63 }

        fun setCustomData64(customData64: String?) = apply { this.customData64 = customData64 }

        fun setCustomData65(customData65: String?) = apply { this.customData65 = customData65 }

        fun setCustomData66(customData66: String?) = apply { this.customData66 = customData66 }

        fun setCustomData67(customData67: String?) = apply { this.customData67 = customData67 }

        fun setCustomData68(customData68: String?) = apply { this.customData68 = customData68 }

        fun setCustomData69(customData69: String?) = apply { this.customData69 = customData69 }

        fun setCustomData70(customData70: String?) = apply { this.customData70 = customData70 }

        fun setCustomData71(customData71: String?) = apply { this.customData71 = customData71 }

        fun setCustomData72(customData72: String?) = apply { this.customData72 = customData72 }

        fun setCustomData73(customData73: String?) = apply { this.customData73 = customData73 }

        fun setCustomData74(customData74: String?) = apply { this.customData74 = customData74 }

        fun setCustomData75(customData75: String?) = apply { this.customData75 = customData75 }

        fun setCustomData76(customData76: String?) = apply { this.customData76 = customData76 }

        fun setCustomData77(customData77: String?) = apply { this.customData77 = customData77 }

        fun setCustomData78(customData78: String?) = apply { this.customData78 = customData78 }

        fun setCustomData79(customData79: String?) = apply { this.customData79 = customData79 }

        fun setCustomData80(customData80: String?) = apply { this.customData80 = customData80 }

        fun setCustomData81(customData81: String?) = apply { this.customData81 = customData81 }

        fun setCustomData82(customData82: String?) = apply { this.customData82 = customData82 }

        fun setCustomData83(customData83: String?) = apply { this.customData83 = customData83 }

        fun setCustomData84(customData84: String?) = apply { this.customData84 = customData84 }

        fun setCustomData85(customData85: String?) = apply { this.customData85 = customData85 }

        fun setCustomData86(customData86: String?) = apply { this.customData86 = customData86 }

        fun setCustomData87(customData87: String?) = apply { this.customData87 = customData87 }

        fun setCustomData88(customData88: String?) = apply { this.customData88 = customData88 }

        fun setCustomData89(customData89: String?) = apply { this.customData89 = customData89 }

        fun setCustomData90(customData90: String?) = apply { this.customData90 = customData90 }

        fun setCustomData91(customData91: String?) = apply { this.customData91 = customData91 }

        fun setCustomData92(customData92: String?) = apply { this.customData92 = customData92 }

        fun setCustomData93(customData93: String?) = apply { this.customData93 = customData93 }

        fun setCustomData94(customData94: String?) = apply { this.customData94 = customData94 }

        fun setCustomData95(customData95: String?) = apply { this.customData95 = customData95 }

        fun setCustomData96(customData96: String?) = apply { this.customData96 = customData96 }

        fun setCustomData97(customData97: String?) = apply { this.customData97 = customData97 }

        fun setCustomData98(customData98: String?) = apply { this.customData98 = customData98 }

        fun setCustomData99(customData99: String?) = apply { this.customData99 = customData99 }

        fun setCustomData100(customData100: String?) = apply { this.customData100 = customData100 }

        fun build(): CustomData {
            return CustomData(
                customData1 = customData1,
                customData2 = customData2,
                customData3 = customData3,
                customData4 = customData4,
                customData5 = customData5,
                customData6 = customData6,
                customData7 = customData7,
                customData8 = customData8,
                customData9 = customData9,
                customData10 = customData10,
                customData11 = customData11,
                customData12 = customData12,
                customData13 = customData13,
                customData14 = customData14,
                customData15 = customData15,
                customData16 = customData16,
                customData17 = customData17,
                customData18 = customData18,
                customData19 = customData19,
                customData20 = customData20,
                customData21 = customData21,
                customData22 = customData22,
                customData23 = customData23,
                customData24 = customData24,
                customData25 = customData25,
                customData26 = customData26,
                customData27 = customData27,
                customData28 = customData28,
                customData29 = customData29,
                customData30 = customData30,
                experimentName = experimentName,
                customData31 = customData31,
                customData32 = customData32,
                customData33 = customData33,
                customData34 = customData34,
                customData35 = customData35,
                customData36 = customData36,
                customData37 = customData37,
                customData38 = customData38,
                customData39 = customData39,
                customData40 = customData40,
                customData41 = customData41,
                customData42 = customData42,
                customData43 = customData43,
                customData44 = customData44,
                customData45 = customData45,
                customData46 = customData46,
                customData47 = customData47,
                customData48 = customData48,
                customData49 = customData49,
                customData50 = customData50,
                customData51 = customData51,
                customData52 = customData52,
                customData53 = customData53,
                customData54 = customData54,
                customData55 = customData55,
                customData56 = customData56,
                customData57 = customData57,
                customData58 = customData58,
                customData59 = customData59,
                customData60 = customData60,
                customData61 = customData61,
                customData62 = customData62,
                customData63 = customData63,
                customData64 = customData64,
                customData65 = customData65,
                customData66 = customData66,
                customData67 = customData67,
                customData68 = customData68,
                customData69 = customData69,
                customData70 = customData70,
                customData71 = customData71,
                customData72 = customData72,
                customData73 = customData73,
                customData74 = customData74,
                customData75 = customData75,
                customData76 = customData76,
                customData77 = customData77,
                customData78 = customData78,
                customData79 = customData79,
                customData80 = customData80,
                customData81 = customData81,
                customData82 = customData82,
                customData83 = customData83,
                customData84 = customData84,
                customData85 = customData85,
                customData86 = customData86,
                customData87 = customData87,
                customData88 = customData88,
                customData89 = customData89,
                customData90 = customData90,
                customData91 = customData91,
                customData92 = customData92,
                customData93 = customData93,
                customData94 = customData94,
                customData95 = customData95,
                customData96 = customData96,
                customData97 = customData97,
                customData98 = customData98,
                customData99 = customData99,
                customData100 = customData100,
            )
        }
    }
}
