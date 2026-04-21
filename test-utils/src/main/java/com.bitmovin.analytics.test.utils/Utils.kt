package com.bitmovin.analytics.test.utils

object Utils {
    fun extractMajorVersion(version: String): Long {
        val splitted = version.split(".")
        return splitted[0].toLong()
    }

    fun extractMinorVersion(version: String): Long {
        val splitted = version.split(".")
        return splitted[1].toLong()
    }
}
