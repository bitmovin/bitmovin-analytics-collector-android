# [![bitmovin](http://bitmovin-a.akamaihd.net/webpages/bitmovin-logo-github.png)](http://www.bitmovin.com)

Android client that allows you to monitor your Bitmovin Native SDK, Media3 ExoPlayer, ExoPlayer or Amazon IVS Player playback with [Bitmovin Analytics](https://bitmovin.com/video-analytics/).

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

#### Integrated Analytics with Bitmovin Player

We recommend to use the integrated analytics that comes with the bitmovin player.
This setup simplifies usage since you don't have to attach/detach the player instance and source metadata is attached directly to the source object.

Check the [Getting Started Guide](https://developer.bitmovin.com/playback/docs/getting-started-android) of the bitmovin player for more information on how to setup the integrated analytics.

#### Standalone Analytics with Bitmovin Player

<table>
<tr>
<td> Player Version </td> <td> Dependency </td>
</tr>

<tr>
<td> v3 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-bitmovin-player:3.17.0'
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

### Media3 ExoPlayer

<table>
<tr>
<td> Player Version </td> <td> Dependency </td>
</tr>
<tr>
<td> >= v1.0.0 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-media3-exoplayer:3.17.0'
}
```

</td>
</tr>
</table>

### ExoPlayer

<table>
<tr>
<td> Player Version </td> <td> Dependency </td>
</tr>
<tr>
<td> >= v2.18.0 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-exoplayer:3.17.0'
}
```

</td>
</tr>

<tr>
<td> < v2.18.0 <br/> >= v2.17.0</td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-exoplayer:2.9.0'
}
```

</td>
</tr>
</table>


### IVS Player

<table>
<tr>
<td> Player Version </td> <td> Dependency </td>
</tr>

<tr>
<td> >= 1.14 </td>
<td>

```gradle
dependencies {
    implementation 'com.bitmovin.analytics:collector-amazon-ivs:3.17.0'
}
```

</td>
</tr>

</table>

## Examples

The following examples create an analyticsCollector and attach a player instance to it.

### Basic analytics monitoring with Bitmovin Player SDK

#### Integrated Analytics with Bitmovin Player

We recommend to use the integrated analytics that comes with the bitmovin player.
This setup simplifies usage since you don't have to attach/detach the player instance and source metadata is attached directly to the source object.

Check the [Getting Started Guide](https://developer.bitmovin.com/playback/docs/getting-started-android) of the bitmovin player for more information on how to setup the integrated analytics.

#### Standalone Analytics with Bitmovin Player

```kotlin
// Create an AnalyticsConfig using your Bitmovin analytics license key (minimal config required)
val analyticsConfig = AnalyticsConfig("<BITMOVIN_ANALYTICS_LICENSE_KEY>")

// Create Analytics Collector for Bitmovin Player
val analyticsCollector = IBitmovinPlayerCollector.Factory.create(getApplicationContext(), analyticsConfig)

// Attach your player instance
analyticsCollector.attachPlayer(player)

// Detach your player when you are done. For example, call this method when you call the release() method
analyticsCollector.detachPlayer()
```

### Basic analytics monitoring with Media3 ExoPlayer

```kotlin
// Create an AnalyticsConfig using your Bitmovin analytics license key (minimal config required)
val analyticsConfig = AnalyticsConfig("<BITMOVIN_ANALYTICS_LICENSE_KEY>")

// Create Analytics Collector for Media3 ExoPlayer
val analyticsCollector = IMedia3ExoPlayerCollector.Factory.create(getApplicationContext(), analyticsConfig)

// Attach your ExoPlayer instance
analyticsCollector.attachPlayer(player)

// Detach your player when you are done. For example, call this method when you call ExoPlayer's release() method
analyticsCollector.detachPlayer()
```

### Basic analytics monitoring with ExoPlayer

```kotlin
// Create an AnalyticsConfig using your Bitmovin analytics license key (minimal config required)
val analyticsConfig = AnalyticsConfig("<BITMOVIN_ANALYTICS_LICENSE_KEY>")

// Create Analytics Collector for ExoPlayer
val analyticsCollector = IExoPlayerCollector.Factory.create(getApplicationContext(), analyticsConfig)

// Attach your ExoPlayer instance
analyticsCollector.attachPlayer(player)

// Detach your player when you are done. For example, call this method when you call ExoPlayer's release() method
analyticsCollector.detachPlayer()
```

### Basic analytics monitoring with Amazon IVS Player SDK

```kotlin
// Create an AnalyticsConfig using your Bitmovin analytics license key (minimal config required)
val analyticsConfig = AnalyticsConfig("<BITMOVIN_ANALYTICS_LICENSE_KEY>")

// Create Analytics Collector for Amazon IVS Player
val analyticsCollector = IAmazonIvsPlayerCollector.Factory.create(getApplicationContext(), analyticsConfig)

// Attach your Amazon IVS Player instance
analyticsCollector.attachPlayer(player)

// Detach your player when you are done. For example, call this method when you call the release() method
analyticsCollector.detachPlayer()
```

### Switching to a new Video with Bitmovin Player SDK

When switching to a new video we recommend that you follow the sequence of events below.

```kotlin

//Detach your player when the first video is completed
analyticsCollector.detachPlayer()

//Set the SourceMetadata for the new source
val sourceMetadata = SourceMetadata(
    title = "newTitle",
    videoId = "newVideoId",

    customData = CustomData(
        customData1 = "genre:action",
    )
)

analyticsCollector.setSourceMetadata(source, sourceMetadata)

//Reattach your player instance
analyticsCollector.attachPlayer(newPlayer)

// Load new source after attaching
player.load(source)
```

### Switching to a new Video with Media3 ExoPlayer, ExoPlayer and Amazon IVS Player SDK

When switching to a new video we recommend that you follow the sequence of events below.

```kotlin

//Detach your player when the first video is completed
analyticsCollector.detachPlayer()

//Set the SourceMetadata for the new source
val sourceMetadata = SourceMetadata(
    title = "newTitle",
    videoId = "newVideoId",

    customData = CustomData(
        customData1 = "genre:action",
    )
)

analyticsCollector.setSourceMetadata(sourceMetadata)

//Reattach your player instance
analyticsCollector.attachPlayer(newPlayer)

//Load new source after attaching
```


### Optional Configuration Parameters

```kotlin
val analyticsConfig = AnalyticsConfig(
    licenseKey = "<BITMOVIN_ANALYTICS_LICENSE_KEY>",
    adTrackingDisabled = false,
    randomizeUserId = false,
    retryPolicy = RetryPolicy.NO_RETRY,
)

// DefaultMetadata is optional metadata that can be used to enrich analytics data with metadata
// that is not source specific. In case the same fields are set on sourceMetadata and defaultMetadata,
// sourceMetadata takes precedence.
val defaultMetadata = DefaultMetadata(
    customUserId = "customUserId",
    cdnProvider = "bitmovin",
    customData = CustomData(
        customData1 = "defaultCustomData1",
        customData2 = "defaultCustomData2",
        customData3 = "defaultCustomData3",
        customData4 = "defaultCustomData4",
        customData5 = "defaultCustomData5",
        experimentName = "experiment-1",
    )
)

// SourceMetadata is optional metadata that can be used to enrich analytics data with metadata
// that is specific to the source.
val sourceMetadata = SourceMetadata(
    title = "videoTitle1234",
    videoId = "videoId1234",
    cdnProvider = CDNProvider.BITMOVIN,
    path = "package.mainactivity",
    isLive = true,
    customData = CustomData(
        customData1 = "sourceCustomData1",
        customData2 = "sourceCustomData2",
        customData3 = "sourceCustomData3",
        customData4 = "sourceCustomData4",
        customData5 = "sourceCustomData5",
        experimentName = "experiment-1",
    ),
)

```

A [full example app](https://github.com/bitmovin/bitmovin-analytics-collector-android/tree/main/collector-bitmovin-player-example) can be seen in the github repo.

For more information about the Analytics Product and the collectors check out our [documentation](https://developer.bitmovin.com/playback/docs/setup-analytics).


## Threading Model

The collector API is not thread safe.
All calls need to come from the same thread as the player is executed on (usually the MainThread).
While the collector might not crash when called from different threads, it can lead to inconsistent data.

## Minification / Obfuscation

Starting with version 3.15.0 there are no longer any specific ProGuard rules required for the Analytics Collector.

## Support

If you have any questions or issues with this Analytics Collector or its examples, or you require other technical support for our services, please login to your Bitmovin Dashboard at [https://bitmovin.com/dashboard](https://bitmovin.com/dashboard) and create a new support case. Our team will get back to you as soon as possible 👍
