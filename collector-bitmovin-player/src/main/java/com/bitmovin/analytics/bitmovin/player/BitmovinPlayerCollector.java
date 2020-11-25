package com.bitmovin.analytics.bitmovin.player;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.data.decorators.DeviceInformationEventDataDecorator;
import com.bitmovin.player.BitmovinPlayer;

public class BitmovinPlayerCollector extends BitmovinAnalytics {

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     */
    public BitmovinPlayerCollector(
            BitmovinAnalyticsConfig bitmovinAnalyticsConfig, Context context) {
        super(bitmovinAnalyticsConfig, context);
    }

    @Deprecated
    public BitmovinPlayerCollector(
            BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.getContext());
    }

    public void attachPlayer(BitmovinPlayer player) {
        DeviceInformationEventDataDecorator deviceInfoEventDataDecorator = new DeviceInformationEventDataDecorator(new DeviceInformationProvider(context, getUserAgent(context)));
        BitmovinSdkAdapter adapter = new BitmovinSdkAdapter(player, this.bitmovinAnalyticsConfig, deviceInfoEventDataDecorator,
                this.playerStateMachine);

        this.attach(adapter);

        if (this.adAnalytics != null) {
            BitmovinSdkAdAdapter adAdapter = new BitmovinSdkAdAdapter(player, this.adAnalytics);
            this.attachAd(adAdapter);
        }
    }

    private String getUserAgent(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        String applicationName = "Unknown";
        if (stringId == 0 && applicationInfo.nonLocalizedLabel != null) {
            applicationInfo.nonLocalizedLabel.toString();
        }
        String versionName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException var5) {
            versionName = "?";
        }

        return applicationName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE + ") " + "BitmovinPlayer/" + BitmovinUtil.getPlayerVersion();
    }
}
