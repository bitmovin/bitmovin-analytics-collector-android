package com.bitmovin.analytics.exoplayer.example.v1;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.AdEventData;
import com.bitmovin.analytics.data.CustomData;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.analytics.example.shared.Sample;
import com.bitmovin.analytics.example.shared.Samples;
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, BitmovinAnalytics.DebugListener, Player.EventListener {
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private Button releaseButton;
    private Button createButton;
    private Button sourceChangeButton;
    private Button setCustomDataButton;
    private TextView eventLogView;
    private final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    private DataSource.Factory dataSourceFactory;

    private ExoPlayerCollector bitmovinAnalytics;
    private BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    private ConcatenatingMediaSource mediaSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.a_main_exoplayer);
        releaseButton = findViewById(R.id.release_button);
        releaseButton.setOnClickListener(this);
        createButton = findViewById(R.id.create_button);
        createButton.setOnClickListener(this);
        sourceChangeButton = findViewById(R.id.source_change_button);
        sourceChangeButton.setOnClickListener(this);
        setCustomDataButton = findViewById(R.id.set_custom_data);
        setCustomDataButton.setOnClickListener(this);
        eventLogView = findViewById(R.id.eventLog);

        dataSourceFactory =
                new DefaultDataSourceFactory(
                        this, bandwidthMeter, buildHttpDataSourceFactory(bandwidthMeter));
        createPlayer();
    }

    private int oldIndex = 0;

    @Override
    public void onPositionDiscontinuity(int reason) {
        int sourceIndex = player.getCurrentWindowIndex();
        if (sourceIndex != oldIndex) {
            if (oldIndex >= 0) {
                mediaSource.removeMediaSource(
                        oldIndex,
                        new Handler(),
                        () -> {
                            Log.d("Mainactivity", "isPlaying: " + player.isPlaying());
                            Log.d("Mainactivity", "playbackState: " + player.getPlaybackState());
                            Log.d("Mainactivity", "playWhenReady: " + player.getPlayWhenReady());
                            bitmovinAnalytics.attachPlayer(player);
                        });
            }
            oldIndex = sourceIndex;
        }
    }

    private void createPlayer() {
        if (player == null) {

            SimpleExoPlayer.Builder exoBuilder = new SimpleExoPlayer.Builder(this);
            exoBuilder.setBandwidthMeter(bandwidthMeter);

            player = exoBuilder.build();
            player.addListener(this);

            // Step 1: Create your analytics config object
            bitmovinAnalyticsConfig =
                    new BitmovinAnalyticsConfig("e73a3577-d91c-4214-9e6d-938fb936818a");

            // Step 2: Add optional parameters
            bitmovinAnalyticsConfig.setVideoId("androidVideoDASHStatic");
            bitmovinAnalyticsConfig.setTitle("Android Bitmovin SDK Video with DASH");
            bitmovinAnalyticsConfig.setCustomUserId("customUserId1");
            bitmovinAnalyticsConfig.setCdnProvider(CDNProvider.BITMOVIN);
            bitmovinAnalyticsConfig.setExperimentName("experiment-timeout");
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
            bitmovinAnalyticsConfig.getConfig().setTryResendDataOnFailedConnection(true);

            eventLogView.setText("");

            // Step 3: Create Analytics Collector
            bitmovinAnalytics =
                    new ExoPlayerCollector(bitmovinAnalyticsConfig, getApplicationContext());

            bitmovinAnalytics.addDebugListener(this);
            this.bitmovinAnalytics = bitmovinAnalytics;

            // Step 4: Attach ExoPlayer
            bitmovinAnalytics.attachPlayer(player);

            // Step 5: Create, prepare, and play media source
            playerView.setPlayer(player);

            MediaSource mediaSource = buildMediaSource(Samples.INSTANCE.getHLS_DRM_WIDEVINE());
            // mediaSource = new ConcatenatingMediaSource(dashMediaSource, dashMediaSource);

            player.prepare(mediaSource);
            player.setPlayWhenReady(false);
        }
    }

    private MediaSource buildMediaSource(Sample sample) {
        Uri uri = sample.getUri();
        int type = Util.inferContentType(uri);

        MediaSourceFactory factory;
        switch (type) {
            case C.TYPE_DASH:
                factory = new DashMediaSource.Factory(dataSourceFactory);
                break;
            case C.TYPE_SS:
                factory = new SsMediaSource.Factory(dataSourceFactory);
                break;
            case C.TYPE_HLS:
                factory = new HlsMediaSource.Factory(dataSourceFactory);
                break;
            case C.TYPE_OTHER:
                factory = new ProgressiveMediaSource.Factory(dataSourceFactory);
                break;
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
        if (sample.getDrmScheme() != null && sample.getDrmLicenseUri() != null) {
            DrmSessionManager<?> drmSessionManager =
                    getDrmSession(
                            sample.getDrmLicenseUri().toString(),
                            Util.getDrmUuid(sample.getDrmScheme()),
                            "ExoPlayerExample");
            factory.setDrmSessionManager(drmSessionManager);
        }

        return factory.createMediaSource(uri);
    }

    protected static DefaultDrmSessionManager getDrmSession(
            String drmLicenseUrl, UUID drmScheme, String userAgent) {

        if (drmLicenseUrl != null && drmScheme != null) {
            try {
                DefaultDrmSessionManager.Builder drmBuilder =
                        new DefaultDrmSessionManager.Builder();
                MediaDrmCallback mediaDrmCallback =
                        createMediaDrmCallback(drmLicenseUrl, userAgent);
                return drmBuilder.build(mediaDrmCallback);
            } catch (Exception e) {
                Log.e("Main Application", e.getMessage());
            }
        }
        return null;
    }

    protected static HttpMediaDrmCallback createMediaDrmCallback(
            String licenseUrl, String userAgent) {
        HttpDataSource.Factory licenseDataSourceFactory =
                new DefaultHttpDataSourceFactory(userAgent);
        HttpMediaDrmCallback drmCallback =
                new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);

        return drmCallback;
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            bitmovinAnalytics.detachPlayer();
            player = null;
        }
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(
            DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(
                Util.getUserAgent(this, getString(R.string.app_name)), bandwidthMeter);
    }

    @Override
    public void onClick(View v) {
        if (v == releaseButton) {
            releasePlayer();
        } else if (v == createButton) {
            createPlayer();
        } else if (v == sourceChangeButton) {
            changeSource();
        } else if (v == setCustomDataButton) {
            setCustomData();
        }
    }

    private void setCustomData() {
        CustomData customData = bitmovinAnalytics.getCustomData();
        customData.setCustomData2("custom_data_2_changed");
        customData.setCustomData4("custom_data_4_changed");
        bitmovinAnalytics.setCustomDataOnce(customData);
    }

    private void changeSource() {
        bitmovinAnalytics.detachPlayer();

        MediaSource mediaSource = buildMediaSource(Samples.INSTANCE.getDASH_DRM_WIDEVINE());
        bitmovinAnalyticsConfig.setVideoId("DRMVideo-id");
        bitmovinAnalyticsConfig.setTitle("DRM Video Title");

        bitmovinAnalytics.attachPlayer(player);
        player.prepare(mediaSource);
    }

    @Override
    public void onDispatchEventData(EventData data) {
        eventLogView.append(
                String.format(
                        "state: %s, duration: %s, time: %s\n",
                        data.getState(), data.getDuration(), data.getTime()));
    }

    @Override
    public void onDispatchAdEventData(AdEventData data) {}

    @Override
    public void onMessage(String message) {}
}