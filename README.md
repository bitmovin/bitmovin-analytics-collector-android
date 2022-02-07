# [![bitmovin](http://bitmovin-a.akamaihd.net/webpages/bitmovin-logo-github.png)](http://www.bitmovin.com)
Android client that allows you to monitor your Bitmovin Native SDK or ExoPlayer playback with [Bitmovin Analytics](https://bitmovin.com/video-analytics/)

# Getting started
## Gradle

Add this to your top level `build.gradle`

```
allprojects {
    repositories {
		maven {
			url  'https://artifacts.bitmovin.com/artifactory/public-releases'
		}
	}
}
```

And this line to your main project `build.gradle`

For Bitmovin Player v2:

```
dependencies {
    implementation 'com.bitmovin.analytics:collector-bitmovin-player:1.30.0'
}
```

For Bitmovin Player v3:

```
dependencies {
    implementation 'com.bitmovin.analytics:collector-bitmovin-player:2.6.0'
}
```

For ExoPlayer < v2.12.0:

```
dependencies {
    implementation 'com.bitmovin.analytics:collector-exoplayer:1.30.0'
}
```

For ExoPlayer >= v2.12.0:

```
dependencies {
    implementation 'com.bitmovin.analytics:collector-exoplayer:2.6.0'
}
```


## Examples

The following example creates a BitmovinAnalytics object and attaches an Bitmovin Native SDK instance to it.

#### Basic analytics monitoring with Bitmovin Player SDK
```java
// Create a BitmovinAnalyticsConfig using your Bitmovin analytics license key and (optionally) your Bitmovin Player Key
BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("<BITMOVIN_ANALYTICS_KEY>", "<BITMOVIN_PLAYER_KEY>");


// Create a BitmovinPlayerCollector object using the BitmovinAnalyitcsConfig you just created
BitmovinPlayerCollector analyticsCollector = new BitmovinPlayerCollector(bitmovinAnalyticsConfig, getApplicationContext());

// Attach your player instance
analyticsCollector.attachPlayer(player);

// Detach your player when you are done. For example, call this method when you call the release() method
analyticsCollector.detachPlayer();
```

#### Basic analytics monitoring with ExoPlayer
```java
// Create a BitmovinAnalyticsConfig using your Bitmovin analytics license key
BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("<BITMOVIN_ANALYTICS_KEY>");

Create Analytics Collector for ExoPlayer
ExoPlayerCollector bitmovinAnalytics = new ExoPlayerCollector(bitmovinAnalyticsConfig, getApplicationContext());

//Attach your ExoPlayer instance
bitmovinAnalytics.attachPlayer(player);

// Detach your player when you are done. For example, call this method when you call ExoPlayer's release() method
analyticsCollector.detachPlayer();
```


#### Switching to a new video 
When switching to a new video we recommend that you follow the sequence of events below. 

```java
//Detach your player when the first video is completed 
analyticsCollector.detachPlayer();

//Update your config with new optional parameters related to the new video playback
bitmovinAnalyticsConfig.setVideoId("newVideoId"); 
bitmovinAnalyticsConfig.setCustomData1("newCustomData"); 

//Reattach your player instance 
analyticsCollector.attachPlayer(newPlayer);
``` 


#### Optional Configuration Parameters
```java
bitmovinAnalyticsConfig.setVideoId("videoId1234"); 
bitmovinAnalyticsConfig.setCustomUserId("customUserId1");
bitmovinAnalyticsConfig.setCdnProvider(CDNProvider.BITMOVIN);
bitmovinAnalyticsConfig.setExperimentName("experiment-1");
bitmovinAnalyticsConfig.setCustomData1("customData1");
bitmovinAnalyticsConfig.setCustomData2("customData2");
bitmovinAnalyticsConfig.setCustomData3("customData3");
bitmovinAnalyticsConfig.setCustomData4("customData4");
bitmovinAnalyticsConfig.setCustomData5("customData5");
bitmovinAnalyticsConfig.setCustomData6("customData6");
bitmovinAnalyticsConfig.setCustomData7("customData7");
bitmovinAnalyticsConfig.setIsLive(false);
bitmovinAnalyticsConfig.setHeartbeatInterval(59700); // value is in ms 

```

A [full example app](https://github.com/bitmovin/bitmovin-analytics-collector-android/tree/main/collector-bitmovin-player-example) can be seen in the github repo

## Support
If you have any questions or issues with this Analytics Collector or its examples, or you require other technical support for our services, please login to your Bitmovin Dashboard at https://bitmovin.com/dashboard and create a new support case. Our team will get back to you as soon as possible 👍
