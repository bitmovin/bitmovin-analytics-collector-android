package com.bitmovin.exoplayeranalyticsexample;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.AdEventData;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
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

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, BitmovinAnalytics.DebugListener {
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private Button releaseButton;
    private Button createButton;
    private TextView eventLogView;
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
        eventLogView = findViewById(R.id.eventLog);

        createPlayer();

//        automationHandler = new Handler();
//
//        automationHandler.postDelayed(new Runnable() {
//            public void run() {
//                releasePlayer();
//                createPlayer();
//                automationHandler.postDelayed(this, automationDelay);
//            }
//        }, automationDelay);

    }

    private void createPlayer() {
        if (player == null) {
            TrackSelection.Factory videoTrackSelectionFactory
                    = new AdaptiveTrackSelection.Factory();
            RenderersFactory renderersFactory = new DefaultRenderersFactory(this);
            player = ExoPlayerFactory.newSimpleInstance(this, renderersFactory,
                    new DefaultTrackSelector(videoTrackSelectionFactory), getDrmSession("https://widevine-proxy.appspot.com/proxy", C.WIDEVINE_UUID));
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, bandwidthMeter,
                    buildHttpDataSourceFactory(bandwidthMeter));

            //Step 1: Create your analytics config object
            BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("e73a3577-d91c-4214-9e6d-938fb936818a");

            //Step 2: Add optional parameters
            bitmovinAnalyticsConfig.setVideoId("androidVideoDASHStatic");
            bitmovinAnalyticsConfig.setTitle("Android Bitmovin SDK Video with DASH");
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
            bitmovinAnalyticsConfig.setPath("/vod/new/");
            bitmovinAnalyticsConfig.setHeartbeatInterval(59700);
            bitmovinAnalyticsConfig.setIsLive(false);

            eventLogView.setText("");
            //Step 3: Create Analytics Collector
            ExoPlayerCollector bitmovinAnalytics = new ExoPlayerCollector(bitmovinAnalyticsConfig, getApplicationContext());
            bitmovinAnalytics.addDebugListener(this);
            this.bitmovinAnalytics = bitmovinAnalytics;

            //Step 4: Attach ExoPlayer
            bitmovinAnalytics.attachPlayer(player);


            //Step 5: Create, prepare, and play media source
            playerView.setPlayer(player);

            Uri dashStatic = Uri.parse("https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd");

            //DASH example
            DashMediaSource dashMediaSource = getMediaSource(dashStatic, dataSourceFactory);
            player.prepare(dashMediaSource);
            player.setPlayWhenReady(true);

        }
    }

    private DrmSessionManager<FrameworkMediaCrypto> getDrmSession(String drmLicenseUrl, UUID drmScheme) {

        if(drmLicenseUrl != null && drmScheme != null) {
            try{
                MediaDrmCallback mediaDrmCallback =
                        createMediaDrmCallback(drmLicenseUrl, null);
                return DefaultDrmSessionManager.newFrameworkInstance(drmScheme, mediaDrmCallback, null);
            } catch (Exception e ){
                Log.e("Main Application", e.getMessage());
            }
        }
        return null;
    }

    private DashMediaSource getMediaSource(Uri dashStatic, DataSource.Factory dataSourceFactory) {
        DashChunkSource.Factory source = new DefaultDashChunkSource.Factory(dataSourceFactory);
        DashMediaSource.Factory sourceFactory = new DashMediaSource.Factory(source, dataSourceFactory);

        return sourceFactory.createMediaSource(dashStatic);
    }

    private HttpMediaDrmCallback createMediaDrmCallback(
            String licenseUrl, String[] keyRequestPropertiesArray) {
        HttpDataSource.Factory licenseDataSourceFactory =
                new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "ExoPlayerExample"));
        HttpMediaDrmCallback drmCallback =
                new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);
        if (keyRequestPropertiesArray != null) {
            for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                        keyRequestPropertiesArray[i + 1]);
            }
        }
        return drmCallback;
    }

    private void releasePlayer() {
        if (player != null) {
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
        if (v == releaseButton) {
            releasePlayer();
        } else if (v == createButton) {
            createPlayer();
        }
    }

    @Override
    public void onDispatchEventData(EventData data)
    {
        eventLogView.append(String.format("state: %s, duration: %s, time: %s\n", data.getState(), data.getDuration(), data.getTime()));
    }

    @Override
    public void onDispatchAdEventData(AdEventData data)
    {

    }

    @Override
    public void onMessage(String message)
    {

    }
}
