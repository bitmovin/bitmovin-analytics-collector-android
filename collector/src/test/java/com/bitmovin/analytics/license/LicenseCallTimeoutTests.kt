package com.bitmovin.analytics.license

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.utils.ClientFactory
import com.bitmovin.analytics.utils.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val LICENSE_KEY = "test-license-key"
private const val GRANTED_BODY = "{\"status\": \"granted\"}"

class LicenseCallTimeoutTests {
    private val server = MockWebServer()
    private val config = AnalyticsConfig(LICENSE_KEY)

    @Before
    fun setup() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun licenseCallReportsErrorWhenTheResponseHeadersOutlastTheCallTimeout() {
        server.enqueue(MockResponse().setBody(GRANTED_BODY).setHeadersDelay(30, TimeUnit.SECONDS))

        val responses = authenticate(ClientFactory().createLicenseClient(config, callTimeoutMillis = 300))

        assertThat(responses).containsExactly(AuthenticationResponse.Error)
    }

    @Test
    fun licenseCallReportsErrorWhenTheResponseBodyOutlastsTheCallTimeout() {
        server.enqueue(MockResponse().setBody(GRANTED_BODY).throttleBody(1, 1, TimeUnit.SECONDS))

        val responses = authenticate(ClientFactory().createLicenseClient(config, callTimeoutMillis = 300))

        assertThat(responses).containsExactly(AuthenticationResponse.Error)
    }

    @Test
    fun licenseCallIsGrantedWithinTheCallTimeout() {
        server.enqueue(MockResponse().setBody(GRANTED_BODY))

        val responses = authenticate(ClientFactory().createLicenseClient(config))

        assertThat(responses).containsExactly(AuthenticationResponse.Granted(LICENSE_KEY, null))
    }

    private fun authenticate(client: OkHttpClient): List<AuthenticationResponse> {
        val responses = CopyOnWriteArrayList<AuthenticationResponse>()
        val completed = CountDownLatch(1)
        val licenseCall =
            DefaultLicenseCall(
                config = config,
                licenseKeyProvider = InstantLicenseKeyProvider(LICENSE_KEY),
                context = mockk(relaxed = true),
                httpClient = HttpClient(mockk(relaxed = true), client),
                backendUrl = server.url("/licensing").toString(),
            )

        runBlocking {
            licenseCall.authenticate {
                responses.add(it)
                completed.countDown()
            }
        }
        completed.await(3, TimeUnit.SECONDS)
        return responses
    }
}
