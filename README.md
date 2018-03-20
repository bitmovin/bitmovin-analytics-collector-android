# [![bitmovin](http://bitmovin-a.akamaihd.net/webpages/bitmovin-logo-github.png)](http://www.bitmovin.com)
Android client that allows you to monitor your Bitmovin Native SDK or ExoPlayer playback with [Bitmovin Analytics](https://bitmovin.com/video-analytics/)

# Getting started
## Gradle

Add this to your top level `build.gradle`

```
allprojects {
    repositories {
		maven {
			url  'http://bitmovin.bintray.com/maven'
		}
	}
}
```

And this line to your main project `build.gradle`

```
dependencies {
    compile 'com.bitmovin.analytics:analytics:1.1.0'
}
```

## Examples

The following example creates a BitmovinAnalytics object and attaches an Bitmovin Native SDK or ExoPlayer instance to it. 

#### Basic analytics monitoring 
```java
// Create a BitmovinAnalyticsConfig using your Bitmovin analytics license key and (optionally) your Bitmovin Player Key

    //Bitmovin Native: 
    BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("<BITMOVIN_ANALYTICS_KEY>", "<BITMOVIN_PLAYER_KEY>", getApplicationContext());

    //ExoPlayer monitoring:
    BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("<BITMOVIN_ANALYTICS_KEY>", getApplicationContext());

// Create a BitmovinAnalytics object using the BitmovinAnalyitcsConfig you just created
BitmovinAnalytics analyticsCollector = new BitmovinAnalytics(bitmovinAnalyticsConfig);

// Attach your player instance
analyticsCollector.attachPlayer(player);

// Detach your player when you are done. For example, call this method when you call ExoPlayer's release() method
bitmovinAnalytics.detachPlayer();
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
bitmovinAnalyticsConfig.setHeartbeatInterval(59700); // value is in ms 

```

A [full example app](https://github.com/bitmovin/bitmovin-analytics-collector-android/tree/master/analyticsexample) can be seen in the github repo 
