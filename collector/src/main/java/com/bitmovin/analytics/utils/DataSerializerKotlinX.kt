package com.bitmovin.analytics.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DataSerializerKotlinX {
    // We use an experimental API here (explicitNulls flag),
    // but this one was promoted to stable with kotlinx.serialization 1.7.0
    // so it is safe to use
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> serialize(data: T?): String? {
        return data?.let {
            // encodeDefaults = true is needed to ensure that all properties (also the ones with defaults)
            // are serialized, this mimics the behavior of Gson which we had before
            // explicitNulls = false is needed to avoid setting of default values
            // for all nullable properties in the data classes
            val json =
                Json {
                    encodeDefaults = true
                    explicitNulls = false
                }
            json.encodeToString(it)
        }
    }

    // We use an experimental API here (explicitNulls flag),
    // but this one was promoted to stable with kotlinx.serialization 1.7.0
    // so it is safe to use
    // clazz is used as parameter to ease inferring of type
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> deserialize(
        input: String?,
        clazz: Class<T>,
    ): T? {
        return input?.let {
            return try {
                val json =
                    Json {
                        encodeDefaults = true
                        explicitNulls = false
                        ignoreUnknownKeys = true // Ignore unknown keys to avoid deserialization errors
                    }
                json.decodeFromString<T>(input)
            } catch (e: SerializationException) {
                BitmovinLog.e("DataSerializerKotlinX", "Failed to deserialize data for class: ${clazz.simpleName} : $e")
                null
            } catch (e: IllegalArgumentException) {
                BitmovinLog.e("DataSerializerKotlinX", "Failed to deserialize data for class: ${clazz.simpleName} : $e")
                null
            }
        }
    }
}
