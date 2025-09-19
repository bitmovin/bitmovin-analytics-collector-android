package com.bitmovin.analytics.utils

import java.io.PrintWriter
import java.io.StringWriter

/**
 * Number of lines to take from the beginning and end of the stack trace.
 * This creates a simplified view focusing on the most relevant parts.
 */
const val STACKTRACE_LINES = 100

/**
 * Extracts stack trace for error tracking, taking first and last 50 lines.
 * Uses printStackTrace to get the common printed format including the Exception itself in the top line.
 * If the stack trace has 100 lines or fewer, returns the complete stack trace.
 * Additional line to show how many lines were truncated is added if more than 100 lines
 */
fun Throwable.extractStackTraceForErrorTracking(): List<String> {
    StringWriter().use { sw ->
        PrintWriter(sw).use { pw ->
            this.printStackTrace(pw)
            val fullStackTraceLines = sw.toString().trim().lines()

            if (fullStackTraceLines.size <= STACKTRACE_LINES) {
                return fullStackTraceLines
            }

            val firstLines = fullStackTraceLines.take(STACKTRACE_LINES / 2)
            val lastLines = fullStackTraceLines.takeLast(STACKTRACE_LINES / 2)
            val removedLinesCount = fullStackTraceLines.size - (STACKTRACE_LINES)
            val truncationIndicator = "... ($removedLinesCount lines removed) ..."

            return firstLines + listOf(truncationIndicator) + lastLines
        }
    }
}
