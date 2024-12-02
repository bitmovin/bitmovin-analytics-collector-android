package com.bitmovin.analytics.utils

import java.io.PrintWriter
import java.io.StringWriter

/*
 * This function is used to get the top of the stacktrace of a throwable.
 * We are using printStackTrace to make sure we get the common printed format
 * of the stacktrace which also includes the Exception itself in the top line.
 */
const val STACKTRACE_LINES_LIMIT = 50

val Throwable.topOfStacktrace: List<String>
    get() {
        StringWriter().use {
                sw ->
            PrintWriter(sw).use {
                    pw ->
                this.printStackTrace(pw)
                val fullStackTraceString = sw.toString().trim()
                val topOfStackTrace = fullStackTraceString.lines().take(STACKTRACE_LINES_LIMIT)
                return topOfStackTrace
            }
        }
    }
