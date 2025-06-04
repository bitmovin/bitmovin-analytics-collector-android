package com.bitmovin.analytics.bitmovin.player.utils

import com.google.gson.Gson

// Gson DataSerializer for Bitmovin Player Analytics
// This one is needed beside kotlinx.serialization
// since the errorevent has Any type for the data field
internal object DataSerializerGson {
    private fun <T> serialize(data: T?): String? {
        return data?.let { Gson().toJson(it) }
    }

    fun <T> trySerialize(data: T?): String? {
        return try {
            serialize(data)
        } catch (ignored: Exception) {
            null
        }
    }
}
