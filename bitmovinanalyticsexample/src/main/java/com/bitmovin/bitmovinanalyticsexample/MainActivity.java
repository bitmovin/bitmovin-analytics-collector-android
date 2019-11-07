package com.bitmovin.bitmovinanalyticsexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerCollector;
import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.config.PlaybackConfiguration;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.track.AudioTrack;

import com.bitmovin.player.config.track.SubtitleTrack;
import java.util.Arrays;
import java.util.List;

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
        this.bitmovinPlayer = this.bitmovinPlayerView.getPlayer();
        PlaybackConfiguration playbackConfiguration = this.bitmovinPlayer.getConfig().getPlaybackConfiguration();
        playbackConfiguration.setMuted(true);
        playbackConfiguration.setAutoplayEnabled(true);

        PlayerConfiguration config = this.bitmovinPlayer.getConfig();
        this.initializeAnalytics();

        this.initializePlayer();

    }

    // This is an internal Bitmovin Analytics Key
    private final String LOCAL_DEVELOPMENT_KEY = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0";

    protected void initializeAnalytics() {

        //Step 1: Create your analytics config object
        BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig(LOCAL_DEVELOPMENT_KEY, "<YOUR_PLAYER_KEY>");

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
        bitmovinAnalyticsConfig.setIsLive(false);

        //Step 3: Create Analytics Collector
        bitmovinAnalytics = new BitmovinPlayerCollector(bitmovinAnalyticsConfig, this.getApplicationContext());
    }


    protected void initializePlayer() {
        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();

        // Add a new source item
        sourceConfiguration.addSourceItem("https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8");

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
}
