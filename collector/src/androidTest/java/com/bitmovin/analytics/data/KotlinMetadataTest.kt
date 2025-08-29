package com.bitmovin.analytics.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bitmovin.analytics.api.AnalyticsCollector
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KotlinMetadataTest {
    // This test verifies that the metadata version of the core collector
    // is stable (we have seen accidental upgrades in the past)
    @Test
    fun verifyMetadata() {
        val metadata = KotlinClassMetadata.readLenient(AnalyticsCollector::class.java.getAnnotation(Metadata::class.java))
        assertThat(metadata.version.toString()).isEqualTo("2.1.0")
    }
}
