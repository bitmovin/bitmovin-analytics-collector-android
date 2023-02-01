package com.bitmovin.analytics.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.BuildConfig
import com.bitmovin.analytics.data.DeviceClass
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.DeviceInformationDto
import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.LegacyErrorData
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.SecureSettingsAndroidIdUserIdProvider
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.features.errordetails.ErrorData
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DataSerializerTest {
    @Test
    fun testSerialize_EventData() {
        val analyticsLicenseKey = "82dc5cdc-d425-4329-a043-b5fc540f9a74"
        val playerLicenseKey = "e32db33d-fd14-4f1c-a5e8-f95341140d04"
        val impressionId = "79b531da-5abb-4fb2-8dbc-9a6c60b6526f"
        val userId = "c54d11c8-dba2-4475-a867-764befdb5ad2"
        val deviceInformation = DeviceInformation("Google", "Pixel 5", false, "user-agent-string", "en_US", "package.bitmovin.com", 640, 280)
        val userIdProvider = mockk<SecureSettingsAndroidIdUserIdProvider>()
        every { userIdProvider.userId() } returns userId

        val errorCode = ErrorCode(1000, "Error Description", ErrorData(), LegacyErrorData("Error Data Message", arrayOf("first line of details", "second line of details")))
        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig(analyticsLicenseKey, playerLicenseKey)
        val eventData = EventDataFactory(bitmovinAnalyticsConfig, userIdProvider).create(impressionId, null, deviceInformation, PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER))

        eventData.m3u8Url = "https://www.mydomain.com/playlist.m3u8"
        eventData.ad = 0
        eventData.errorCode = errorCode.errorCode
        eventData.errorData = DataSerializer.serialize(errorCode.legacyErrorData)
        eventData.time = 1607598943236

        assertThat(DataSerializer.serialize(eventData)).isEqualTo("{\"impressionId\":\"79b531da-5abb-4fb2-8dbc-9a6c60b6526f\",\"userId\":\"c54d11c8-dba2-4475-a867-764befdb5ad2\",\"key\":\"82dc5cdc-d425-4329-a043-b5fc540f9a74\",\"playerKey\":\"e32db33d-fd14-4f1c-a5e8-f95341140d04\",\"userAgent\":\"user-agent-string\",\"deviceInformation\":{\"manufacturer\":\"Google\",\"model\":\"Pixel 5\",\"isTV\":false},\"language\":\"en_US\",\"analyticsVersion\":\"${BuildConfig.COLLECTOR_CORE_VERSION}\",\"playerTech\":\"Android:Exoplayer\",\"domain\":\"package.bitmovin.com\",\"screenHeight\":640,\"screenWidth\":280,\"isLive\":false,\"isCasting\":false,\"videoDuration\":0,\"time\":1607598943236,\"videoWindowWidth\":0,\"videoWindowHeight\":0,\"droppedFrames\":0,\"played\":0,\"buffered\":0,\"paused\":0,\"ad\":0,\"seeked\":0,\"videoPlaybackWidth\":0,\"videoPlaybackHeight\":0,\"videoBitrate\":0,\"audioBitrate\":0,\"videoTimeStart\":0,\"videoTimeEnd\":0,\"videoStartupTime\":0,\"duration\":0,\"startupTime\":0,\"errorCode\":1000,\"errorData\":\"{\\\"msg\\\":\\\"Error Data Message\\\",\\\"details\\\":[\\\"first line of details\\\",\\\"second line of details\\\"]}\",\"playerStartupTime\":0,\"pageLoadType\":1,\"pageLoadTime\":0,\"m3u8Url\":\"https://www.mydomain.com/playlist.m3u8\",\"isMuted\":false,\"sequenceNumber\":0,\"platform\":\"android\",\"subtitleEnabled\":false,\"videoStartFailed\":false,\"retryCount\":0,\"player\":\"exoplayer\"}")
    }

    @Test
    fun testSerialize_DeviceClass() {
        val deviceInformation = DeviceInformationDto("Amazon", "FireTv", true, deviceClass = DeviceClass.Tablet)
        val eventData = mockk<EventData>()
        every { eventData.deviceInformation }.returns(deviceInformation)
        assertThat(DataSerializer.serialize(eventData)).contains("\"deviceClass\":\"Tablet\"")
    }

    @Test
    fun testSerialize_EventDataWithNullErrorData() {
        val analyticsLicenseKey = "82dc5cdc-d425-4329-a043-b5fc540f9a74"
        val playerLicenseKey = "e32db33d-fd14-4f1c-a5e8-f95341140d04"
        val impressionId = "79b531da-5abb-4fb2-8dbc-9a6c60b6526f"
        val userId = "c54d11c8-dba2-4475-a867-764befdb5ad2"
        val deviceInformation = DeviceInformation("Google", "Pixel 5", false, "user-agent-string", "en_US", "package.bitmovin.com", 640, 280)
        val userIdProvider = mockk<SecureSettingsAndroidIdUserIdProvider>()
        every { userIdProvider.userId() } returns userId

        val errorCode = ErrorCode(1000, "Error Description", ErrorData(), null)
        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig(analyticsLicenseKey, playerLicenseKey)
        val eventData = EventDataFactory(bitmovinAnalyticsConfig, userIdProvider).create(impressionId, null, deviceInformation, PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER))

        eventData.m3u8Url = "https://www.mydomain.com/playlist.m3u8"
        eventData.ad = 0
        eventData.errorCode = errorCode.errorCode
        eventData.errorData = DataSerializer.serialize(null)
        eventData.time = 1607598943236
        assertThat(DataSerializer.serialize(eventData)).isEqualTo("{\"impressionId\":\"79b531da-5abb-4fb2-8dbc-9a6c60b6526f\",\"userId\":\"c54d11c8-dba2-4475-a867-764befdb5ad2\",\"key\":\"82dc5cdc-d425-4329-a043-b5fc540f9a74\",\"playerKey\":\"e32db33d-fd14-4f1c-a5e8-f95341140d04\",\"userAgent\":\"user-agent-string\",\"deviceInformation\":{\"manufacturer\":\"Google\",\"model\":\"Pixel 5\",\"isTV\":false},\"language\":\"en_US\",\"analyticsVersion\":\"${BuildConfig.COLLECTOR_CORE_VERSION}\",\"playerTech\":\"Android:Exoplayer\",\"domain\":\"package.bitmovin.com\",\"screenHeight\":640,\"screenWidth\":280,\"isLive\":false,\"isCasting\":false,\"videoDuration\":0,\"time\":1607598943236,\"videoWindowWidth\":0,\"videoWindowHeight\":0,\"droppedFrames\":0,\"played\":0,\"buffered\":0,\"paused\":0,\"ad\":0,\"seeked\":0,\"videoPlaybackWidth\":0,\"videoPlaybackHeight\":0,\"videoBitrate\":0,\"audioBitrate\":0,\"videoTimeStart\":0,\"videoTimeEnd\":0,\"videoStartupTime\":0,\"duration\":0,\"startupTime\":0,\"errorCode\":1000,\"playerStartupTime\":0,\"pageLoadType\":1,\"pageLoadTime\":0,\"m3u8Url\":\"https://www.mydomain.com/playlist.m3u8\",\"isMuted\":false,\"sequenceNumber\":0,\"platform\":\"android\",\"subtitleEnabled\":false,\"videoStartFailed\":false,\"retryCount\":0,\"player\":\"exoplayer\"}")
    }

    @Test
    fun testSerialize_serializesNullObject() {
        val serializedData = DataSerializer.serialize(null)

        assertThat(serializedData).isEqualTo(null)
    }
}
