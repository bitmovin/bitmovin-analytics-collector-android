package com.bitmovin.analytics.utils

import android.util.Log

class SystemInformationProvider {

    companion object {
        fun getProperty(property: String): String? {
            return try {
                System.getProperty(property)
            } catch (e: Exception) {
                Log.e("SystemInformationPrvd", "Something went wrong while getting system property, e: ", e)
                return null
            }
        }
    }
}
