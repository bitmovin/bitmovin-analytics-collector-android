# [![bitmovin](http://bitmovin-a.akamaihd.net/webpages/bitmovin-logo-github.png)](http://www.bitmovin.com)
Android client that allows you to monitor your ExoPlayer playback with [Bitmovin Analytics](https://bitmovin.com/video-analytics/)

# Getting started
## Gradle
Add this to your build.gradle:
```
dependencies {
    compile project(path: ':bitmovinanalyticscollector')
}
```

## Examples

The following example creates a BitmovinAnalytics object and attaches an ExoPlayer instance to it. 

```java
// Create a BitmovinAnalyticsConfig using your Bitmovin analytics license key and your Bitmovin Player Key
BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("<BITMOVIN_ANALYTICS_KEY>", "<BITMOVIN_PLAYER_KEY>", getApplicationContext());

// Create a BitmovinAnalytics object using the BitmovinAnalyitcsConfig you just created
BitmovinAnalytics analyticsCollector = new BitmovinAnalytics(bitmovinAnalyticsConfig);

// Attach your player instance
analyticsCollector.attachPlayer(exoPlayer);

// Detach your player when you are done. For example, call this method when you call ExoPlayer's release() method
bitmovinAnalytics.detachPlayer();
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

A [full example app](https://github.com/bitmovin/bitmovin-analytics-exoplayer-private/blob/master/exoplayeranalyticsexample/src/main/java/com/bitmovin/exoplayeranalyticsexample/MainActivity.java) can be seen in the github repo 
