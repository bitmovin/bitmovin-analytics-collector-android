# [![bitmovin](http://bitmovin-a.akamaihd.net/webpages/bitmovin-logo-github.png)](http://www.bitmovin.com)
Android client that allows you to monitor your ExoPlayer playback with (Bitmovin Analytics)[https://bitmovin.com/video-analytics/]

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
BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("<BITMOVIN_ANALYTICS_KEY>", "<BITMOVIN_PLAYER_KEY>", this);

//Create a BitmovinAnalytics object using the BitmovinAnalyitcsConfig you just created
BitmovinAnalytics analyticsCollector = new BitmovinAnalytics(bitmovinAnalyticsConfig);

//Attach your ExoPlayer instance
analyticsCollector.attachPlayer(exoPlayer);

```


A [full example app](https://github.com/bitmovin/bitmovin-analytics-exoplayer-private/blob/master/exoplayeranalyticsexample/src/main/java/com/bitmovin/exoplayeranalyticsexample/MainActivity.java) can be seen in the github repo 
