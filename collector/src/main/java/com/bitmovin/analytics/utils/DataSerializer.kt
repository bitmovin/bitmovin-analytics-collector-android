package com.bitmovin.analytics.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

object DataSerializer {
    @JvmStatic
    fun <T> serialize(data: T): String {
        val gson = Gson()
        return gson.toJson(data)
    }

    @JvmStatic
    fun <T> deserialize(input: String?, classOfT: Class<T>?): T? {
        var response: T? = null
        return try {
            response = Gson().fromJson(input, classOfT)
            response
        } catch (e: JsonSyntaxException) {
            response
        }
    }
}
