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
        customData29 = customData29, customData30 = customData30, experimentName = experimentName, null,
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
            )
        }
    }
}
