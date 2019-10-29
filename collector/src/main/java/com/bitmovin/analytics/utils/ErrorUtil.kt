package com.bitmovin.analytics.utils

val Throwable.topOfStacktrace : Array<String>
get() = this.stackTrace.take(10).map { it.toString() }.toTypedArray()


