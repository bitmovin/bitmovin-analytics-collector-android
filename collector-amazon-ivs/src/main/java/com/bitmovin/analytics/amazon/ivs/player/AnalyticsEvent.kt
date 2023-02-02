package com.bitmovin.analytics.amazon.ivs.player

import org.json.JSONObject

class AnalyticsEvent(json: String) : JSONObject(json) {
    val url = this.optString("url")
}
