package com.bitmovin.analytics.utils

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.RetryPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ClientFactoryTest {
    @Test
    fun licenseClientTimesOutTheWholeCallAfter60Seconds() {
        RetryPolicy.values().forEach { retryPolicy ->
            val client = ClientFactory().createLicenseClient(AnalyticsConfig("key", retryPolicy = retryPolicy))

            assertThat(client.callTimeoutMillis).describedAs(retryPolicy.name).isEqualTo(60_000)
        }
    }

    @Test
    fun sampleClientHasNoCallTimeout() {
        RetryPolicy.values().forEach { retryPolicy ->
            val client = ClientFactory().createClient(AnalyticsConfig("key", retryPolicy = retryPolicy))

            assertThat(client.callTimeoutMillis).describedAs(retryPolicy.name).isEqualTo(0)
        }
    }
}
