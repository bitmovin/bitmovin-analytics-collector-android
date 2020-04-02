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
import com.bitmovin.player.config.drm.DRMSystems;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.advertising.AdItem;
import com.bitmovin.player.config.advertising.AdSource;
import com.bitmovin.player.config.advertising.AdSourceType;
import com.bitmovin.player.config.advertising.AdvertisingConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.config.track.AudioTrack;

import com.bitmovin.player.config.track.SubtitleTrack;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BitmovinPlayerView bitmovinPlayerView;
    private BitmovinPlayer bitmovinPlayer;
    private BitmovinPlayerCollector bitmovinAnalytics;
    private Button releaseButton;
    private Button createButton;
    private Button changeSource;
    private Button changeAudio;
    private Button changeSubtitle;

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
        changeAudio = findViewById(R.id.change_audio);
        changeAudio.setOnClickListener(this);
        changeSubtitle = findViewById(R.id.change_subtitle);
        changeAudio.setOnClickListener(this);

        this.bitmovinPlayerView = this.findViewById(R.id.bitmovinPlayerView);

        this.bitmovinAnalytics = this.initializeAnalytics();

        this.initializeBitmovinPlayer();
    }

    protected void initializeBitmovinPlayer(){
        PlayerConfiguration config = new PlayerConfiguration();

//        SourceConfiguration source = this.createSource();
        SourceConfiguration source = this.createDRMSource();
        config.setSourceConfiguration(source);

//        AdvertisingConfiguration adConfig = initializeAds(config);
//        config.setAdvertisingConfiguration(adConfig);

        PlaybackConfiguration playbackConfiguration = config.getPlaybackConfiguration();
        playbackConfiguration.setMuted(true);
        playbackConfiguration.setAutoplayEnabled(true);

        this.bitmovinPlayer = new BitmovinPlayer(getApplicationContext(), config);

        this.bitmovinAnalytics.detachPlayer();
        this.bitmovinAnalytics.attachPlayer(bitmovinPlayer);

        this.bitmovinPlayerView.setPlayer(this.bitmovinPlayer);
    }

    protected BitmovinPlayerCollector initializeAnalytics() {
        //Step 1: Create your analytics config object with the Local Development Key
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
        bitmovinAnalyticsConfig.setCustomData6("customData6");
        bitmovinAnalyticsConfig.setCustomData7("customData7");
        bitmovinAnalyticsConfig.setPath("/vod/new/");
        bitmovinAnalyticsConfig.setHeartbeatInterval(59700);
        bitmovinAnalyticsConfig.setAds(true);
        bitmovinAnalyticsConfig.setIsLive(false);

        //Step 3: Create Analytics Collector
        return new BitmovinPlayerCollector(bitmovinAnalyticsConfig, getApplicationContext());
    }


    protected SourceConfiguration createSource() {
        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();
        SourceItem sourceItem = new SourceItem("https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8");

        // Add a new source item
        sourceConfiguration.addSourceItem(sourceItem);

        return sourceConfiguration;
    }

    protected SourceConfiguration createDRMSource() {
        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();
        SourceItem sourceItem = new SourceItem("https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd");

        // setup DRM handling
        String drmLicenseUrl = "https://widevine-proxy.appspot.com/proxy";
        UUID drmSchemeUuid = DRMSystems.WIDEVINE_UUID;
        sourceItem.addDRMConfiguration(drmSchemeUuid, drmLicenseUrl);

        // Add a new source item
        sourceConfiguration.addSourceItem(sourceItem);

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
            initializeBitmovinPlayer();
        } else if (v == changeSource) {
            onPlayerChangeSource();
        } else if (v == changeAudio) {
            onAudioTrackChange();
        } else if (v == changeSubtitle) {
            onSubtitleChange();
        }
    }

    private void onPlayerChangeSource() {
        SourceConfiguration config = new SourceConfiguration();
        config.addSourceItem("http://bitdash-a.akamaihd.net/content/sintel/sintel.mpd");
        bitmovinPlayer.load(config);

    }

    private void onAudioTrackChange() {
        AudioTrack[] available = bitmovinPlayer.getAvailableAudio();
        List<AudioTrack> audioTracks = Arrays.asList(available);
        int index = audioTracks.indexOf(bitmovinPlayer.getAudio());

        String id = available[(index + 1) % available.length].getId();
        bitmovinPlayer.setAudio(id);
    }

    private void onSubtitleChange() {
        SubtitleTrack[] available = bitmovinPlayer.getAvailableSubtitles();
        List<SubtitleTrack> audioTracks = Arrays.asList(available);
        int index = audioTracks.indexOf(bitmovinPlayer.getSubtitle());

        String id = available[(index + 1) % available.length].getId();
        bitmovinPlayer.setSubtitle(id);
    }

    private static final String AD_SOURCE_1 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirecterror&nofb=1&correlator=";
    private static final String AD_SOURCE_2 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator=";
    private static final String AD_SOURCE_3 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator=";
    private static final String AD_SOURCE_4 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirectlinear&correlator=";
    private static final String AD_SOURCE_5 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirecterror&nofb=1&correlator=";


    private AdvertisingConfiguration initializeAds(PlayerConfiguration config) {
        // Create AdSources
        AdSource firstAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_1);
        AdSource secondAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_2);
        AdSource thirdAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_3);
        AdSource fourthAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_4);
        AdSource fifthAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_5);

        // Setup a pre-roll ad
//        AdItem preRoll = new AdItem("pre", firstAdSource);
//        AdItem preRoll = new AdItem("pre", secondAdSource);
        AdItem preRoll = new AdItem("pre", thirdAdSource);
//        AdItem preRoll = new AdItem("pre", fourthAdSource);
//        AdItem preRoll = new AdItem("pre", fifthAdSource);
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
