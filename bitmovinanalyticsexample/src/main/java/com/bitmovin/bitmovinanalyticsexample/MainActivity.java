package com.bitmovin.bitmovinanalyticsexample;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.bitmovin.analytics.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.config.media.SourceConfiguration;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private BitmovinPlayerView bitmovinPlayerView;
    private BitmovinPlayer bitmovinPlayer;
    private BitmovinAnalytics bitmovinAnalytics;
    private Button releaseButton;
    private Button createButton;
    private Handler automationHandler;
    private int automationDelay = 90000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        releaseButton = findViewById(R.id.release_button);
        releaseButton.setOnClickListener(this);
        createButton = findViewById(R.id.create_button);
        createButton.setOnClickListener(this);

        this.bitmovinPlayerView = this.findViewById(R.id.bitmovinPlayerView);
        this.bitmovinPlayer = this.bitmovinPlayerView.getPlayer();
        this.bitmovinPlayer.getConfig().getPlaybackConfiguration().setAutoplayEnabled(true);

        this.initializeAnalytics();

        this.initializePlayer();

//        automationHandler = new Handler();
//
//        automationHandler.postDelayed(new Runnable(){
//            public void run(){
//                releasePlayer();
//                initializePlayer();
//                automationHandler.postDelayed(this, automationDelay);
//            }
//        }, automationDelay);
    }

    protected void initializeAnalytics(){
        //Step 1: Create your analytics config object
        BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("<YOUR_ANALYTICS_KEY>", "<YOUR_PLAYER_KEY>", getApplicationContext());

        //Step 2: Add optional parameters
        bitmovinAnalyticsConfig.setVideoId("androidVideoDASHStatic");
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

        //Step 3: Create Analytics Collector
        bitmovinAnalytics = new BitmovinAnalytics(bitmovinAnalyticsConfig);
    }


    protected void initializePlayer() {
        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();

        // Add a new source item
        sourceConfiguration.addSourceItem("https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd");


        //Step 4: Attach BitmovinPlayer
        bitmovinAnalytics.attachPlayer(bitmovinPlayer);

        // load source using the created source configuration
        bitmovinPlayer.load(sourceConfiguration);
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
            initializePlayer();
        }
    }

}
