package com.bitmovin.bitmovinanalyticsexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerCollector;
import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.config.PlaybackConfiguration;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.advertising.AdItem;
import com.bitmovin.player.config.advertising.AdSource;
import com.bitmovin.player.config.advertising.AdSourceType;
import com.bitmovin.player.config.advertising.AdvertisingConfiguration;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BitmovinPlayerView bitmovinPlayerView;
    private BitmovinPlayer bitmovinPlayer;
    private BitmovinPlayerCollector bitmovinAnalytics;
    private Button releaseButton;
    private Button createButton;
    private Button changeSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        releaseButton = findViewById(R.id.release_button);
        releaseButton.setOnClickListener(this);
        createButton = findViewById(R.id.create_button);
        createButton.setOnClickListener(this);
        changeSource = findViewById(R.id.change_source);
        changeSource.setOnClickListener(this);

        this.bitmovinPlayerView = this.findViewById(R.id.bitmovinPlayerView);

        PlayerConfiguration config = new PlayerConfiguration();

        SourceConfiguration source = this.createSource();
        config.setSourceConfiguration(source);

        AdvertisingConfiguration adConfig = initializeAds(config);
        config.setAdvertisingConfiguration(adConfig);

        PlaybackConfiguration playbackConfiguration = config.getPlaybackConfiguration();
        playbackConfiguration.setMuted(true);
        playbackConfiguration.setAutoplayEnabled(false);

        this.bitmovinAnalytics = this.initializeAnalytics();

        this.bitmovinPlayer = new BitmovinPlayer(getApplicationContext(), config);
        this.bitmovinAnalytics.attachPlayer(bitmovinPlayer);

        this.bitmovinPlayerView.setPlayer(this.bitmovinPlayer);


    }

    protected BitmovinPlayerCollector initializeAnalytics() {
        //Step 1: Create your analytics config object
        BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0");

        //Step 2: Add optional parameters
        bitmovinAnalyticsConfig.setVideoId("androidVideoDASHStatic");
        bitmovinAnalyticsConfig.setTitle("Android ExoPlayer Video with DASH");
        bitmovinAnalyticsConfig.setCustomUserId("customBitmovinUserId1");
        bitmovinAnalyticsConfig.setCdnProvider(CDNProvider.BITMOVIN);
        bitmovinAnalyticsConfig.setExperimentName("experiment-1");
        bitmovinAnalyticsConfig.setCustomData1("customData1");
        bitmovinAnalyticsConfig.setCustomData2("customData2");
        bitmovinAnalyticsConfig.setCustomData3("customData3");
        bitmovinAnalyticsConfig.setCustomData4("customData4");
        bitmovinAnalyticsConfig.setCustomData5("customData5");
        bitmovinAnalyticsConfig.setPath("/vod/new/");
        bitmovinAnalyticsConfig.setHeartbeatInterval(59700);
        bitmovinAnalyticsConfig.setAds(true);

        //Step 3: Create Analytics Collector
        return new BitmovinPlayerCollector(bitmovinAnalyticsConfig, getApplicationContext());
    }


    protected SourceConfiguration createSource() {
        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();

        // Add a new source item
        sourceConfiguration.addSourceItem("https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd");

        return sourceConfiguration;
    }

    @Override
    protected void onResume() {
        super.onResume();
        bitmovinPlayerView.onResume();
    }

    @Override
    protected void onPause() {
        bitmovinPlayerView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        bitmovinPlayerView.onDestroy();
        super.onDestroy();
    }

    private void releasePlayer() {
        if (bitmovinPlayer != null) {
            bitmovinPlayer.unload();
            bitmovinAnalytics.detachPlayer();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == releaseButton) {
            releasePlayer();
        } else if (v == createButton) {
            createSource();
        } else if (v == changeSource) {
            onPlayerChangeSource();
        }
    }

    private void onPlayerChangeSource() {
        SourceConfiguration config = new SourceConfiguration();
        config.addSourceItem("http://bitdash-a.akamaihd.net/content/sintel/sintel.mpd");
        bitmovinPlayer.load(config);
    }
    private static final String AD_SOURCE_1 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirecterror&nofb=1&correlator=";
    private static final String AD_SOURCE_2 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator=";
    private static final String AD_SOURCE_3 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator=";
    private static final String AD_SOURCE_4 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirectlinear&correlator=";


    private AdvertisingConfiguration initializeAds(PlayerConfiguration config) {
        // Create AdSources
        AdSource firstAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_1);
        AdSource secondAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_2);
        AdSource thirdAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_3);
        AdSource fourthAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_4);

        // Setup a pre-roll ad
        AdItem preRoll = new AdItem("pre", thirdAdSource);
        // Setup a mid-roll waterfalling ad at 10% of the content duration
        // NOTE: AdItems containing more than one AdSource, will be executed as waterfalling ad
        AdItem midRoll = new AdItem("10%", firstAdSource, secondAdSource);
        // Setup a post-roll ad
        AdItem postRoll = new AdItem("post", fourthAdSource);

        // Add the AdItems to the AdvertisingConfiguration
        AdvertisingConfiguration advertisingConfiguration = new AdvertisingConfiguration(preRoll, midRoll, postRoll);
        return advertisingConfiguration;
    }
}
