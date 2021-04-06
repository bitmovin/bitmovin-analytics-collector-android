package com.bitmovin.analytics.bitmovinplayer.example;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerCollector;
import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.player.PlayerView;
import com.bitmovin.player.api.PlaybackConfig;
import com.bitmovin.player.api.Player;
import com.bitmovin.player.api.PlayerConfig;
import com.bitmovin.player.api.advertising.AdItem;
import com.bitmovin.player.api.advertising.AdSource;
import com.bitmovin.player.api.advertising.AdSourceType;
import com.bitmovin.player.api.advertising.AdvertisingConfig;
import com.bitmovin.player.api.drm.WidevineConfig;
import com.bitmovin.player.api.media.audio.AudioTrack;
import com.bitmovin.player.api.media.subtitle.SubtitleTrack;
import com.bitmovin.player.api.source.SourceConfig;
import java.util.List;

// SVARGA: convert to kotlin
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private PlayerView bitmovinPlayerView;
    private Player bitmovinPlayer;
    private BitmovinPlayerCollector bitmovinAnalytics;
    private Button releaseButton;
    private Button createButton;
    private Button changeSource;
    private Button changeAudio;
    private Button changeSubtitle;
    private BitmovinAnalyticsConfig bitmovinAnalyticsConfig;

    private final SourceConfig sintelSourceConfig =
            new SourceConfig("https://bitmovin-a.akamaihd.net/content/sintel/sintel.mpd");
    private final SourceConfig corruptedSourceConfig =
            new SourceConfig(
                    "https://bitmovin-a.akamaihd.net/content/analytics-teststreams/redbull-parkour/corrupted_first_segment.mpd");

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

    protected void initializeBitmovinPlayer() {
        PlaybackConfig playbackConfig = new PlaybackConfig();
        playbackConfig.setMuted(false);
        playbackConfig.setAutoplayEnabled(false);

        PlayerConfig config = new PlayerConfig();
        config.setPlaybackConfig(playbackConfig);
        config.setAdvertisingConfig(createAdvertisingConfig());

        bitmovinPlayer = Player.create(getApplicationContext(), config);

        this.bitmovinAnalytics.detachPlayer();
        this.bitmovinAnalytics.attachPlayer(bitmovinPlayer);

        bitmovinPlayer.load(sintelSourceConfig);

        this.bitmovinPlayerView.setPlayer(this.bitmovinPlayer);
    }

    protected BitmovinPlayerCollector initializeAnalytics() {
        // Step 1: Create your analytics config object with the Local Development Key
        bitmovinAnalyticsConfig =
                new BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0");

        // Step 2: Add optional parameters
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
        bitmovinAnalyticsConfig.setAds(false);
        bitmovinAnalyticsConfig.setIsLive(false);

        // Step 3: Create Analytics Collector
        return new BitmovinPlayerCollector(bitmovinAnalyticsConfig, getApplicationContext());
    }

    protected static SourceConfig createDRMSourceConfig() {
        // Create a new source config
        SourceConfig sourceConfig =
                SourceConfig.fromUrl(
                        "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd");

        // Attach DRM handling to the source config
        sourceConfig.setDrmConfig(new WidevineConfig("https://widevine-proxy.appspot.com/proxy"));

        return sourceConfig;
    }

    @Override
    protected void onStart() {
        bitmovinPlayerView.onStart();
        super.onStart();
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
    protected void onStop() {
        bitmovinPlayerView.onStop();
        super.onStop();
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
        bitmovinAnalytics.detachPlayer();

        SourceConfig sourceConfig = createDRMSourceConfig();
        bitmovinAnalyticsConfig.setVideoId("DRMVideo-id");
        bitmovinAnalyticsConfig.setTitle("DRM Video Title");

        bitmovinAnalytics.attachPlayer(bitmovinPlayer);
        bitmovinPlayer.load(sourceConfig);
    }

    private void onAudioTrackChange() {
        List<AudioTrack> audioTracks = bitmovinPlayer.getAvailableAudio();
        int index = audioTracks.indexOf(bitmovinPlayer.getAudio());

        int nextIndex = (index + 1) % audioTracks.size();
        String id = audioTracks.get(nextIndex).getId();
        bitmovinPlayer.setAudio(id);
    }

    private void onSubtitleChange() {
        List<SubtitleTrack> subtitleTracks = bitmovinPlayer.getAvailableSubtitles();
        int index = subtitleTracks.indexOf(bitmovinPlayer.getSubtitle());

        int nextIndex = (index + 1) % subtitleTracks.size();
        String id = subtitleTracks.get(nextIndex).getId();
        bitmovinPlayer.setSubtitle(id);
    }

    private static AdvertisingConfig createAdvertisingConfig() {
        // These are IMA Sample Tags from
        // https://developers.google.com/interactive-media-ads/docs/sdks/android/tags
        String AD_SOURCE_1 =
                "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirecterror&nofb=1&correlator=";
        String AD_SOURCE_2 =
                "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator=";
        String AD_SOURCE_3 =
                "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator=";
        String AD_SOURCE_4 =
                "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirectlinear&correlator=";

        // Create AdSources
        AdSource firstAdSource = new AdSource(AdSourceType.Ima, AD_SOURCE_1);
        AdSource secondAdSource = new AdSource(AdSourceType.Ima, AD_SOURCE_2);
        AdSource thirdAdSource = new AdSource(AdSourceType.Ima, AD_SOURCE_3);
        AdSource fourthAdSource = new AdSource(AdSourceType.Ima, AD_SOURCE_4);

        // Set up a pre-roll ad
        AdItem preRoll = new AdItem("pre", thirdAdSource);

        // Set up a mid-roll waterfalling ad at 10% of the content duration
        // NOTE: AdItems containing more than one AdSource will be executed as waterfalling ad
        AdItem midRoll = new AdItem("10%", firstAdSource, secondAdSource);

        // Set up a post-roll ad
        AdItem postRoll = new AdItem("post", fourthAdSource);

        // Add the AdItems to the AdvertisingConfiguration
        return new AdvertisingConfig(preRoll, midRoll, postRoll);
    }
}
