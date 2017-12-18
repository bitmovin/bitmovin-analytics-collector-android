package com.bitmovin.exoplayeranalyticsexample;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bitmovin.bitmovinanalyticscollector.analytics.BitmovinAnalytics;
import com.bitmovin.bitmovinanalyticscollector.utils.BitmovinAnalyticsConfig;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

public class MainActivity extends AppCompatActivity {


    private SimpleExoPlayer player;
    private SimpleExoPlayerView simpleExoPlayerView;
    private static final DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.a_main_exoplayer);

        createPlayer();
        attachPlayerView();
        preparePlayer();
    }

    public void createPlayer() {
        TrackSelection.Factory videoTrackSelectionFactory
                = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        RenderersFactory renderersFactory = new DefaultRenderersFactory(this);
        player = ExoPlayerFactory.newSimpleInstance(renderersFactory,
                new DefaultTrackSelector(videoTrackSelectionFactory), new DefaultLoadControl());

        BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac", "18ca6ad5-9768-4129-bdf6-17685e0d14d2", this);
        BitmovinAnalytics analyticsCollector = new BitmovinAnalytics(bitmovinAnalyticsConfig);
        analyticsCollector.attachPlayer(player);
    }

    public void attachPlayerView() {
        simpleExoPlayerView.setPlayer(player);
    }

    public void preparePlayer() {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));

        Uri uri = Uri.parse("http://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd");

        DashMediaSource dashMediaSource = new DashMediaSource(uri, dataSourceFactory,
                new DefaultDashChunkSource.Factory(dataSourceFactory), null, null);

        player.prepare(dashMediaSource);

    }

    private HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(this,
                getString(R.string.app_name)), bandwidthMeter);
    }
}
