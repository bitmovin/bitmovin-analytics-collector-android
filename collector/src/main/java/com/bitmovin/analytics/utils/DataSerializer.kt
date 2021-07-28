package com.bitmovin.analytics.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

object DataSerializer {
    @JvmStatic
    fun <T> serialize(data: T?): String? {
        return data?.let { Gson().toJson(it) }
    }

    @JvmStatic
    fun <T> trySerialize(data: T?): String? {
        return try {
            serialize(data)
        } catch(ignored: Exception) { null }
    }

    @JvmStatic
    fun <T> deserialize(input: String?, classOfT: Class<T>?): T? {
        return input?.let {
            return try {
                Gson().fromJson(input, classOfT)
            } catch (e: JsonSyntaxException) {
                null
            }
        }
    }
}
