package com.bitmovin.analytics.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DataSerializerKotlinX {
    // We use an experimental API here (explicitNulls flag),
    // but this one was promoted to stable with kotlinx.serialization 1.7.0
    // so it is safe to use
    // We are reusing the instance as recommended in the documentation:
    // https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#json-configuration

    // for encoding
    // encodeDefaults = true is needed to ensure that all properties (also the ones with defaults)
    //      are serialized, this mimics the behavior of Gson which we had before
    // explicitNulls = false makes sure that null values are not serialized and thus reduces request size

    // for decoding
    // encodeDefaults = true is needed to ensure that all properties (also the ones with defaults)
    //      are serialized, this mimics the behavior of Gson which we had before
    // explicitNulls = false is needed to avoid setting of default values
    //      for all nullable properties in the data classes
    // ignoreUnknownKeys = true this is needed for backward compatibility,
    //      in case we introduce new properties in the data classes
    @OptIn(ExperimentalSerializationApi::class)
    val jsonInstance: Json =
        Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }

    inline fun <reified T> serialize(data: T?): String? {
        return data?.let {
            jsonInstance.encodeToString(it)
        }
    }

    // clazz is used as parameter to ease inferring of type
    inline fun <reified T> deserialize(
        input: String?,
        clazz: Class<T>,
    ): T? {
        return input?.let {
            return try {
                jsonInstance.decodeFromString<T>(input)
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
