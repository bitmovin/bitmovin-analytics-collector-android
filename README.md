# [![bitmovin](http://bitmovin-a.akamaihd.net/webpages/bitmovin-logo-github.png)](http://www.bitmovin.com)

Android client that allows you to monitor your Bitmovin Native SDK, ExoPlayer or Amazon IVS Player playback with [Bitmovin Analytics](https://bitmovin.com/video-analytics/).

# Getting started

## Gradle

Add this to your top level `build.gradle`

```gradle
allprojects {
    repositories {
        mavenCentral()
        google()
    
        maven {
            url  'https://artifacts.bitmovin.com/artifactory/public-releases'
        }
    }
}
```

And this line, depending on your player version, to your main project `build.gradle` :

### Bitmovin Player

<table>
<tr>
<td> Player Version </td> <td> Dependency </td>
</tr>

<tr>
<td> v3 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-bitmovin-player:2.15.0'
}
```

</td>
</tr>

<tr>
<td> v2 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-bitmovin-player:1.36.0'
}
```

</td>
</tr>

</table>

### Exoplayer

<table>
<tr>
<td> Player Version </td> <td> Dependency </td>
</tr>
<tr>
<td> >= v2.18.0 and <= v2.18.5 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-exoplayer:2.15.0'
}
```

</td>
</tr>

<tr>
<td> >= v2.17.0 and < v2.18.0 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-exoplayer:2.9.0'
}
```

</td>
</tr>

<tr>
<td> >= v2.12.0 and < v2.17.0 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-exoplayer:2.6.2'
}
```

</td>
</tr>

<tr>
<td> < v2.12.0 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-exoplayer:1.36.0'
}
```

</td>
</tr>
</table>

### IVS Player

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-amazon-ivs:2.15.0'
}
```

## Examples

The following example creates a BitmovinAnalyticsCollector object and attaches a Player instance to it.

### Basic analytics monitoring with Bitmovin Player SDK

```kotlin
// Create a BitmovinAnalyticsConfig using your Bitmovin analytics license key and (optionally) your Bitmovin Player Key
val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("<BITMOVIN_ANALYTICS_KEY>", "<BITMOVIN_PLAYER_KEY>")

// Create a BitmovinPlayerCollector object using the BitmovinAnalyitcsConfig you just created
val analyticsCollector = IBitmovinPlayerCollector.Factory.create(bitmovinAnalyticsConfig, getApplicationContext())

// Attach your player instance
analyticsCollector.attachPlayer(player)

// Detach your player when you are done. For example, call this method when you call the release() method
analyticsCollector.detachPlayer()
```

### Basic analytics monitoring with ExoPlayer

```kotlin
// Create a BitmovinAnalyticsConfig using your Bitmovin analytics license key
val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("<BITMOVIN_ANALYTICS_KEY>")

// Create Analytics Collector for ExoPlayer
val analyticsCollector = IExoPlayerCollector.Factory.create(bitmovinAnalyticsConfig, getApplicationContext())

// Attach your ExoPlayer instance
analyticsCollector.attachPlayer(player)

// Detach your player when you are done. For example, call this method when you call ExoPlayer's release() method
analyticsCollector.detachPlayer()
```

### Basic analytics monitoring with Amazon IVS Player SDK

```kotlin
// Create a BitmovinAnalyticsConfig using your Bitmovin analytics license key
val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("<BITMOVIN_ANALYTICS_KEY>")

// Create Analytics Collector for Amazon IVS Player
val analyticsCollector = IAmazonIvsPlayerCollector.Factory.create(bitmovinAnalyticsConfig, getApplicationContext())

// Attach your Amazon IVS Player instance
analyticsCollector.attachPlayer(player)

// Detach your player when you are done. For example, call this method when you call the release() method
analyticsCollector.detachPlayer()
```

### Switching to a new video

When switching to a new video we recommend that you follow the sequence of events below.

```kotlin
//Detach your player when the first video is completed
analyticsCollector.detachPlayer()

//Update your config with new optional parameters related to the new video playback
bitmovinAnalyticsConfig.videoId = "newVideoId"
bitmovinAnalyticsConfig.customData1 = "newCustomData"

//Reattach your player instance
analyticsCollector.attachPlayer(newPlayer)
```

### Optional Configuration Parameters

```kotlin
bitmovinAnalyticsConfig.title = "videoTitle1234"
bitmovinAnalyticsConfig.videoId = "videoId1234"
bitmovinAnalyticsConfig.cdnProvider = CDNProvider.BITMOVIN
bitmovinAnalyticsConfig.isLive= false
bitmovinAnalyticsConfig.experimentName = "experiment-1"
bitmovinAnalyticsConfig.customUserId = "customUserId1"
bitmovinAnalyticsConfig.customData1 = "customData1"
bitmovinAnalyticsConfig.customData2 = "customData2"
bitmovinAnalyticsConfig.customData3 = "customData3"
bitmovinAnalyticsConfig.customData4 = "customData4"
bitmovinAnalyticsConfig.customData5 = "customData5"
bitmovinAnalyticsConfig.path = "path"
bitmovinAnalyticsConfig.ads = false
bitmovinAnalyticsConfig.randomizeUserId = false
```

A [full example app](https://github.com/bitmovin/bitmovin-analytics-collector-android/tree/main/collector-bitmovin-player-example) can be seen in the github repo.

For more information about the Analytics Product and the collectors check out our [documentation](https://developer.bitmovin.com/playback/docs/setup-analytics).

## Support

If you have any questions or issues with this Analytics Collector or its examples, or you require other technical support for our services, please login to your Bitmovin Dashboard at [https://bitmovin.com/dashboard](https://bitmovin.com/dashboard) and create a new support case. Our team will get back to you as soon as possible 👍
