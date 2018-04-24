package com.bitmovin.exoplayeranalyticsexample;

import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.bitmovin.analytics.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.analytics.analytics.BitmovinAnalyticsConfig;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private Button releaseButton;
    private Button createButton;
    private static final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    private BitmovinAnalytics bitmovinAnalytics;
    private Handler automationHandler;
    private int automationDelay = 90000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.a_main_exoplayer);
        releaseButton = findViewById(R.id.release_button);
        releaseButton.setOnClickListener(this);
        createButton = findViewById(R.id.create_button);
        createButton.setOnClickListener(this);

        createPlayer();

        automationHandler = new Handler();

        automationHandler.postDelayed(new Runnable(){
            public void run(){
                releasePlayer();
                createPlayer();
                automationHandler.postDelayed(this, automationDelay);
            }
        }, automationDelay);

    }

    private void createPlayer() {
        if(player==null) {
            TrackSelection.Factory videoTrackSelectionFactory
                    = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            RenderersFactory renderersFactory = new DefaultRenderersFactory(this);
            player = ExoPlayerFactory.newSimpleInstance(renderersFactory,
                    new DefaultTrackSelector(videoTrackSelectionFactory), new DefaultLoadControl());
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, bandwidthMeter,
                    buildHttpDataSourceFactory(bandwidthMeter));

            //Step 1: Create your analytics config object
            BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("<YOUR_ANALYTICS_KEY>", getApplicationContext());

            //Step 2: Add optional parameters
            bitmovinAnalyticsConfig.setVideoId("androidVideoDASHStatic");
            bitmovinAnalyticsConfig.setCustomUserId("customUserId1");
            bitmovinAnalyticsConfig.setCdnProvider(CDNProvider.BITMOVIN);
            bitmovinAnalyticsConfig.setExperimentName("experiment-1");
            bitmovinAnalyticsConfig.setCustomData1("customData1");
            bitmovinAnalyticsConfig.setCustomData2("customData2");
            bitmovinAnalyticsConfig.setCustomData3("customData3");
            bitmovinAnalyticsConfig.setCustomData4("customData4");
            bitmovinAnalyticsConfig.setCustomData5("customData5");
            bitmovinAnalyticsConfig.setPath("/vod/new/");
            bitmovinAnalyticsConfig.setHeartbeatInterval(59700);

            //Step 3: Create Analytics Colelctor
            bitmovinAnalytics = new BitmovinAnalytics(bitmovinAnalyticsConfig);

            //Step 4: Attach ExoPlayer
            bitmovinAnalytics.attachPlayer(player);

            //Step 5: Create, prepeare, and play media source
            playerView.setPlayer(player);

            Random r = new Random();
            int randomInt = r.nextInt(100) + 1;

            Uri dashStatic = Uri.parse("http://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd");
            if(randomInt >= 100) {
                dashStatic = Uri.parse("http://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-88adfadfa99-f0f6155f6efa.mpd");
            }
            //DASH example
            DashChunkSource.Factory source = new DefaultDashChunkSource.Factory(dataSourceFactory);
            DashMediaSource dashMediaSource = new DashMediaSource.Factory(source,dataSourceFactory).createMediaSource(dashStatic, new Handler(),null);
            player.prepare(dashMediaSource);
            player.setPlayWhenReady(true);

        }
    }

    private void releasePlayer(){
        if(player != null){
            player.release();
            bitmovinAnalytics.detachPlayer();
            player = null;
        }
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(this,
                getString(R.string.app_name)), bandwidthMeter);
    }

    @Override
    public void onClick(View v) {
        if (v == releaseButton){
            releasePlayer();
        }else if (v == createButton){
            createPlayer();
        }
    }
}
