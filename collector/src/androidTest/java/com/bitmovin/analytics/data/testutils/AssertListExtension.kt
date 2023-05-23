package com.bitmovin.analytics.data.testutils

import org.assertj.core.api.Condition
import org.assertj.core.api.ListAssert
import java.util.function.Predicate

internal fun <ELEMENT> ListAssert<ELEMENT>.areAtLeast(
    count: Int,
    description: String = "",
    predicate: (ELEMENT) -> Boolean,
): ListAssert<ELEMENT> = areAtLeast(count, Condition(Predicate(predicate), description))
