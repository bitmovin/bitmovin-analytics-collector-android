package com.bitmovin.analytics

import com.bitmovin.analytics.data.CustomData

class CustomDataHelpers {
    interface Getter {
        fun getCustomData(): CustomData
    }
    interface Setter {
        fun setCustomData(customData: CustomData)
    }
}
