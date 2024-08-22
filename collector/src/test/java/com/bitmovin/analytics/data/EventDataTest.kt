package com.bitmovin.analytics.data

import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.Util
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import java.util.UUID

class EventDataTest {
    private val licenseKey = UUID.randomUUID().toString()
    private val impressionId = UUID.randomUUID().toString()
    private val userId = UUID.randomUUID().toString()

    private lateinit var analyticsConfig: AnalyticsConfig

    @Before
    fun setup() {
        analyticsConfig = AnalyticsConfig(licenseKey = licenseKey)
    }

    @Test
    fun testEventDataContainsDeviceInformation() {
        val deviceInformation =
            DeviceInformation("myManufacturer", "myModel", false, "de", "package-name", 100, 200)
        val eventData =
            TestFactory.createEventDataFactory(analyticsConfig).create(
                impressionId,
                SourceMetadata(),
                DefaultMetadata(),
                deviceInformation,
                PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
                null,
            )

        assertThat(eventData.deviceInformation.manufacturer).isEqualTo("myManufacturer")
        assertThat(eventData.deviceInformation.model).isEqualTo("myModel")
        assertThat(eventData.deviceInformation.isTV).isFalse()
        assertThat(eventData.language).isEqualTo("de")
        assertThat(eventData.domain).isEqualTo("package-name")
        assertThat(eventData.screenHeight).isEqualTo(100)
        assertThat(eventData.screenWidth).isEqualTo(200)
        assertThat(eventData.platform).isEqualTo("android")
    }

    @Test
    fun testEventDataSetsPlatformToAndroidTVIfDeviceInformationIsTVIsTrue() {
        val deviceInformation =
            DeviceInformation("myManufacturer", "myModel", true, "de", "package-name", 100, 200)
        val eventData =
            TestFactory.createEventDataFactory(analyticsConfig).create(
                impressionId,
                SourceMetadata(),
                DefaultMetadata(),
                deviceInformation,
                PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
                null,
            )

        assertThat(eventData.platform).isEqualTo("androidTV")
    }

    @Test
    fun testEventDataSetsRandomizedUserId() {
        val deviceInformation =
            DeviceInformation("myManufacturer", "myModel", true, "de", "package-name", 100, 200)
        var randomizedUserIdProvider = RandomizedUserIdIdProvider()
        var randomizedUserIdProvider1 = RandomizedUserIdIdProvider()
        var eventData =
            TestFactory.createEventDataFactory(
                analyticsConfig,
                randomizedUserIdProvider,
            ).create(
                impressionId,
                SourceMetadata(),
                DefaultMetadata(),
                deviceInformation,
                PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
                null,
            )
        var eventData1 =
            TestFactory.createEventDataFactory(
                analyticsConfig,
                randomizedUserIdProvider,
            ).create(
                impressionId,
                SourceMetadata(),
                DefaultMetadata(),
                deviceInformation,
                PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
                null,
            )
        var eventData2 =
            TestFactory.createEventDataFactory(
                analyticsConfig,
                randomizedUserIdProvider1,
            ).create(
                impressionId,
                SourceMetadata(),
                DefaultMetadata(),
                deviceInformation,
                PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
                null,
            )

        assertThat(eventData.userId).isEqualTo(eventData1.userId)
        assertThat(eventData.userId).isEqualTo(randomizedUserIdProvider.userId())
        assertThat(eventData2.userId).isEqualTo(randomizedUserIdProvider1.userId())
        assertThat(eventData2.userId).isNotEqualTo(eventData1.userId)
    }

    @Test
    fun testEventDataStringIsDeserialized() {
        // this test ensures that we are backward compatible in case EventData class is changing
        // arrange
        val eventData1Json =
            """
            {"ad":0,"analyticsVersion":"0.0.0-local","audioBitrate":-1,"audioCodec":"mp4a.40.2","audioLanguage":"en","buffered":0,"cdnProvider":"testCdnProvider","customData1":"systemtest","customData10":"customData10","customData11":"customData11","customData12":"customData12","customData13":"customData13","customData14":"customData14","customData15":"customData15","customData16":"customData16","customData17":"customData17","customData18":"customData18","customData19":"customData19","customData2":"customData2","customData20":"customData20","customData21":"customData21","customData22":"customData22","customData23":"customData23","customData24":"customData24","customData25":"customData25","customData26":"customData26","customData27":"customData27","customData28":"customData28","customData29":"customData29","customData3":"customData3","customData30":"customData30","customData4":"customData4","customData5":"customData5","customData6":"customData6","customData7":"customData7","customData8":"systemtest8","customData9":"customData9","customUserId":"customBitmovinUserId1","deviceInformation":{"isTV":false,"manufacturer":"Google","model":"sdk_gphone64_arm64"},"domain":"com.bitmovin.analytics.bitmovin.player.test","droppedFrames":0,"duration":3929,"experimentName":"experiment-1","impressionId":"c6e0e073-c777-49f2-a7e1-e47bc8a839cb","isCasting":false,"isLive":false,"isMuted":false,"key":"17e6ea02-cb5a-407f-9d6b-9400358fbcc0","language":"en_US","m3u8Url":"https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8","mpdUrl":"https://test.com/mpd","pageLoadTime":0,"pageLoadType":1,"path":"/customPath/new/","paused":0,"platform":"android","played":0,"player":"bitmovin","playerKey":"dummyplayerKey","playerStartupTime":1,"playerTech":"Android:Exoplayer","progUrl":"https://test.com/prog","retryCount":0,"screenHeight":2209,"screenWidth":1080,"seeked":0,"sequenceNumber":0,"startupTime":3929,"state":"startup","streamFormat":"hls","subtitleEnabled":false,"supportedVideoCodecs":["hevc","vp9","avc"],"time":1689091210921,"userAgent":"Dalvik/2.1.0 (Linux; U; Android 13; sdk_gphone64_arm64 Build/TE1A.220922.029)","userId":"25f572b30123c015","version":"bitmovin-3.38.0","videoBitrate":628000,"videoCodec":"avc1.4D400D","videoDuration":210000,"videoId":"dummy-videoId","videoPlaybackHeight":180,"videoPlaybackWidth":320,"videoStartFailed":false,"videoStartupTime":3928,"videoTimeEnd":0,"videoTimeStart":0,"videoTitle":"offlineSession","videoWindowHeight":123,"videoWindowWidth":123}
            """.trimIndent()

        // act
        val eventData1 = DataSerializer.deserialize(eventData1Json, EventData::class.java)

        // assert
        if (eventData1 == null) {
            fail<Nothing>("eventData1Json couldn't be deserialized")
            return
        }

        assertThat(eventData1.ad).isEqualTo(0)
        assertThat(eventData1.analyticsVersion).isEqualTo("0.0.0-local")
        assertThat(eventData1.audioBitrate).isEqualTo(-1)
        assertThat(eventData1.audioCodec).isEqualTo("mp4a.40.2")
        assertThat(eventData1.audioLanguage).isEqualTo("en")
        assertThat(eventData1.buffered).isEqualTo(0)
        assertThat(eventData1.cdnProvider).isEqualTo("testCdnProvider")
        assertThat(eventData1.customData1).isEqualTo("systemtest")
        assertThat(eventData1.customData2).isEqualTo("customData2")
        assertThat(eventData1.customData3).isEqualTo("customData3")
        assertThat(eventData1.customData4).isEqualTo("customData4")
        assertThat(eventData1.customData5).isEqualTo("customData5")
        assertThat(eventData1.customData6).isEqualTo("customData6")
        assertThat(eventData1.customData7).isEqualTo("customData7")
        assertThat(eventData1.customData8).isEqualTo("systemtest8")
        assertThat(eventData1.customData9).isEqualTo("customData9")
        assertThat(eventData1.customData10).isEqualTo("customData10")
        assertThat(eventData1.customData11).isEqualTo("customData11")
        assertThat(eventData1.customData12).isEqualTo("customData12")
        assertThat(eventData1.customData13).isEqualTo("customData13")
        assertThat(eventData1.customData14).isEqualTo("customData14")
        assertThat(eventData1.customData15).isEqualTo("customData15")
        assertThat(eventData1.customData16).isEqualTo("customData16")
        assertThat(eventData1.customData17).isEqualTo("customData17")
        assertThat(eventData1.customData18).isEqualTo("customData18")
        assertThat(eventData1.customData19).isEqualTo("customData19")
        assertThat(eventData1.customData20).isEqualTo("customData20")
        assertThat(eventData1.customData21).isEqualTo("customData21")
        assertThat(eventData1.customData22).isEqualTo("customData22")
        assertThat(eventData1.customData23).isEqualTo("customData23")
        assertThat(eventData1.customData24).isEqualTo("customData24")
        assertThat(eventData1.customData25).isEqualTo("customData25")
        assertThat(eventData1.customData26).isEqualTo("customData26")
        assertThat(eventData1.customData27).isEqualTo("customData27")
        assertThat(eventData1.customData28).isEqualTo("customData28")
        assertThat(eventData1.customData29).isEqualTo("customData29")
        assertThat(eventData1.customData30).isEqualTo("customData30")
        assertThat(eventData1.customUserId).isEqualTo("customBitmovinUserId1")
        assertThat(eventData1.droppedFrames).isEqualTo(0)
        assertThat(eventData1.domain).isEqualTo("com.bitmovin.analytics.bitmovin.player.test")
        assertThat(eventData1.customUserId).isEqualTo("customBitmovinUserId1")
        assertThat(eventData1.deviceInformation.isTV).isFalse
        assertThat(eventData1.deviceInformation.manufacturer).isEqualTo("Google")
        assertThat(eventData1.deviceInformation.model).isEqualTo("sdk_gphone64_arm64")
        assertThat(eventData1.domain).isEqualTo("com.bitmovin.analytics.bitmovin.player.test")
        assertThat(eventData1.duration).isEqualTo(3929)
        assertThat(eventData1.experimentName).isEqualTo("experiment-1")
        assertThat(eventData1.impressionId).isEqualTo("c6e0e073-c777-49f2-a7e1-e47bc8a839cb")
        assertThat(eventData1.isCasting).isFalse
        assertThat(eventData1.isLive).isFalse
        assertThat(eventData1.isMuted).isFalse
        assertThat(eventData1.key).isEqualTo("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")
        assertThat(eventData1.language).isEqualTo("en_US")
        assertThat(
            eventData1.m3u8Url,
        ).isEqualTo("https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8")
        assertThat(eventData1.mpdUrl).isEqualTo("https://test.com/mpd")
        assertThat(eventData1.pageLoadTime).isEqualTo(0)
        assertThat(eventData1.pageLoadType).isEqualTo(1)
        assertThat(eventData1.path).isEqualTo("/customPath/new/")
        assertThat(eventData1.paused).isEqualTo(0)
        assertThat(eventData1.platform).isEqualTo("android")
        assertThat(eventData1.played).isEqualTo(0)
        assertThat(eventData1.player).isEqualTo("bitmovin")
        assertThat(eventData1.playerKey).isEqualTo("dummyplayerKey")
        assertThat(eventData1.playerStartupTime).isEqualTo(1)
        assertThat(eventData1.playerTech).isEqualTo("Android:Exoplayer")
        assertThat(eventData1.progUrl).isEqualTo("https://test.com/prog")
        assertThat(eventData1.retryCount).isEqualTo(0)
        assertThat(eventData1.screenHeight).isEqualTo(2209)
        assertThat(eventData1.screenWidth).isEqualTo(1080)
        assertThat(eventData1.seeked).isEqualTo(0)
        assertThat(eventData1.sequenceNumber).isEqualTo(0)
        assertThat(eventData1.startupTime).isEqualTo(3929)
        assertThat(eventData1.state).isEqualTo("startup")
        assertThat(eventData1.streamFormat).isEqualTo("hls")
        assertThat(eventData1.subtitleEnabled).isFalse
        assertThat(eventData1.supportedVideoCodecs).isEqualTo(listOf("hevc", "vp9", "avc"))
        assertThat(eventData1.time).isEqualTo(1689091210921)
        assertThat(eventData1.userAgent).isEqualTo("Dalvik/2.1.0 (Linux; U; Android 13; sdk_gphone64_arm64 Build/TE1A.220922.029)")
        assertThat(eventData1.userId).isEqualTo("25f572b30123c015")
        assertThat(eventData1.version).isEqualTo("bitmovin-3.38.0")
        assertThat(eventData1.videoBitrate).isEqualTo(628000)
        assertThat(eventData1.videoCodec).isEqualTo("avc1.4D400D")
        assertThat(eventData1.videoDuration).isEqualTo(210000)
        assertThat(eventData1.videoId).isEqualTo("dummy-videoId")
        assertThat(eventData1.videoPlaybackHeight).isEqualTo(180)
        assertThat(eventData1.videoPlaybackWidth).isEqualTo(320)
        assertThat(eventData1.videoStartFailed).isFalse
        assertThat(eventData1.videoStartupTime).isEqualTo(3928)
        assertThat(eventData1.videoTimeEnd).isEqualTo(0)
        assertThat(eventData1.videoTimeStart).isEqualTo(0)
        assertThat(eventData1.videoTitle).isEqualTo("offlineSession")
        assertThat(eventData1.videoWindowHeight).isEqualTo(123)
        assertThat(eventData1.videoWindowWidth).isEqualTo(123)
    }

    @Test
    fun testEventDataIsCorrectlySerialized() {
        val eventData =
            EventData(
                deviceInfo =
                    DeviceInformation(
                        manufacturer = "manufacturer",
                        model = "model",
                        isTV = false,
                        locale = "locale",
                        domain = "packageName",
                        screenHeight = 2400,
                        screenWidth = 1080,
                    ),
                playerInfo = PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
                customData =
                    CustomData(
                        customData1 = "customData1",
                        customData2 = "customData2",
                        customData3 = "customData3",
                        customData4 = "customData4",
                        customData5 = "customData5",
                        customData6 = "customData6",
                        customData7 = "customData7",
                        customData8 = "customData8",
                        customData9 = "customData9",
                        customData10 = "customData10",
                        customData11 = "customData11",
                        customData12 = "customData12",
                        customData13 = "customData13",
                        customData14 = "customData14",
                        customData15 = "customData15",
                        customData16 = "customData16",
                        customData17 = "customData17",
                        customData18 = "customData18",
                        customData19 = "customData19",
                        customData20 = "customData20",
                        customData21 = "customData21",
                        customData22 = "customData22",
                        customData23 = "customData23",
                        customData24 = "customData24",
                        customData25 = "customData25",
                        customData26 = "customData26",
                        customData27 = "customData27",
                        customData28 = "customData28",
                        customData29 = "customData29",
                        customData30 = "customData30",
                        experimentName = "experimentName",
                    ),
                impressionId = "impressionId",
                userId = "userId",
                key = "licenseKey",
                videoId = "videoId",
                customUserId = "customUserId",
                path = "path",
                cdnProvider = "cdnProvider",
                userAgent = "userAgent",
                videoTitle = "videoTitle",
            ).apply {
                time = 1234567890
                drmLoadTime = 1234
                videoDuration = 2
                videoWindowWidth = 3
                videoWindowHeight = 4
                droppedFrames = 5
                played = 6
                buffered = 7
                paused = 8
                ad = 9
                seeked = 10
                videoPlaybackWidth = 11
                videoPlaybackHeight = 12
                videoBitrate = 13
                audioBitrate = 14
                videoTimeStart = 15
                videoTimeEnd = 16
                videoStartupTime = 17
                duration = 18
                startupTime = 19
                playerStartupTime = 20
                pageLoadType = 1
                pageLoadTime = 21
                isMuted = false
                sequenceNumber = 22
                state = "state"
                errorCode = 23
                errorMessage = "errorMessage"
                errorData = "errorData"
                streamFormat = "streamFormat"
                mpdUrl = "mpdUrl"
                m3u8Url = "m3u8Url"
                progUrl = "progUrl"
                videoCodec = "videoCodec"
                audioCodec = "audioCodec"
                supportedVideoCodecs = listOf("supportedVideoCodecs")
                subtitleEnabled = false
                subtitleLanguage = "subtitleLanguage"
                audioLanguage = "audioLanguage"
                drmType = "drmType"
                isLive = false
                isCasting = false
                castTech = "castTech"
                videoStartFailed = false
                retryCount = 24
                playerKey = "playerKey"
            }

        val serializedEventData = DataSerializer.serialize(eventData)
        assertThat(serializedEventData)
            .isEqualTo(
                "{\"impressionId\":\"impressionId\",\"userId\":\"userId\",\"key\":\"licenseKey\"," +
                    "\"videoId\":\"videoId\",\"videoTitle\":\"videoTitle\"," +
                    "\"customUserId\":\"customUserId\",\"path\":\"path\"," +
                    "\"cdnProvider\":\"cdnProvider\",\"userAgent\":\"userAgent\"," +
                    "\"deviceInformation\":{\"manufacturer\":\"manufacturer\"," +
                    "\"model\":\"model\",\"isTV\":false},\"language\":\"locale\"," +
                    "\"analyticsVersion\":\"" + Util.analyticsVersion + "\"," +
                    "\"playerTech\":\"Android:Exoplayer\"," +
                    "\"domain\":\"packageName\",\"screenHeight\":2400,\"screenWidth\":1080," +
                    "\"isLive\":false,\"isCasting\":false,\"castTech\":\"castTech\"," +
                    "\"videoDuration\":2,\"time\":1234567890,\"videoWindowWidth\":3," +
                    "\"videoWindowHeight\":4,\"droppedFrames\":5,\"played\":6,\"buffered\":7," +
                    "\"paused\":8,\"ad\":9,\"seeked\":10,\"videoPlaybackWidth\":11," +
                    "\"videoPlaybackHeight\":12,\"videoBitrate\":13,\"audioBitrate\":14," +
                    "\"videoTimeStart\":15,\"videoTimeEnd\":16,\"videoStartupTime\":17," +
                    "\"duration\":18,\"startupTime\":19,\"state\":\"state\",\"errorCode\":23," +
                    "\"errorMessage\":\"errorMessage\",\"errorData\":\"errorData\"," +
                    "\"playerStartupTime\":20,\"pageLoadType\":1,\"pageLoadTime\":21," +
                    "\"streamFormat\":\"streamFormat\",\"mpdUrl\":\"mpdUrl\"," +
                    "\"m3u8Url\":\"m3u8Url\",\"progUrl\":\"progUrl\",\"isMuted\":false," +
                    "\"sequenceNumber\":22,\"platform\":\"android\"," +
                    "\"videoCodec\":\"videoCodec\",\"audioCodec\":\"audioCodec\"," +
                    "\"supportedVideoCodecs\":[\"supportedVideoCodecs\"]," +
                    "\"subtitleEnabled\":false,\"subtitleLanguage\":\"subtitleLanguage\"," +
                    "\"audioLanguage\":\"audioLanguage\",\"drmType\":\"drmType\"," +
                    "\"drmLoadTime\":1234,\"videoStartFailed\":false,\"retryCount\":24," +
                    "\"player\":\"exoplayer\",\"playerKey\":\"playerKey\"," +
                    "\"customData1\":\"customData1\",\"customData2\":\"customData2\"," +
                    "\"customData3\":\"customData3\",\"customData4\":\"customData4\"," +
                    "\"customData5\":\"customData5\",\"customData6\":\"customData6\"," +
                    "\"customData7\":\"customData7\",\"customData8\":\"customData8\"," +
                    "\"customData9\":\"customData9\",\"customData10\":\"customData10\"," +
                    "\"customData11\":\"customData11\",\"customData12\":\"customData12\"," +
                    "\"customData13\":\"customData13\",\"customData14\":\"customData14\"," +
                    "\"customData15\":\"customData15\",\"customData16\":\"customData16\"," +
                    "\"customData17\":\"customData17\",\"customData18\":\"customData18\"," +
                    "\"customData19\":\"customData19\",\"customData20\":\"customData20\"," +
                    "\"customData21\":\"customData21\",\"customData22\":\"customData22\"," +
                    "\"customData23\":\"customData23\",\"customData24\":\"customData24\"," +
                    "\"customData25\":\"customData25\",\"customData26\":\"customData26\"," +
                    "\"customData27\":\"customData27\",\"customData28\":\"customData28\"," +
                    "\"customData29\":\"customData29\",\"customData30\":\"customData30\"," +
                    "\"experimentName\":\"experimentName\"}",
            )
    }
}
