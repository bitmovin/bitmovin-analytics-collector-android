package com.bitmovin.analytics.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.media.MediaCodecList
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Pair
import com.bitmovin.analytics.BuildConfig
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.StreamFormat
import java.net.URI
import java.util.UUID
import kotlin.math.roundToInt

object Util {
    const val MILLISECONDS_IN_SECONDS = 1000
    const val VIDEOSTART_TIMEOUT = 1000 * 60 // in milliseconds
    const val ANALYTICS_QUALITY_CHANGE_COUNT_THRESHOLD = 50
    const val ANALYTICS_QUALITY_CHANGE_COUNT_RESET_INTERVAL = 1000 * 60 * 60 // in milliseconds;
    const val REBUFFERING_TIMEOUT = 1000 * 60 * 2 // in milliseconds
    const val HEARTBEAT_INTERVAL = 59700 // in milliseconds
    private val VIDEO_FORMAT_MIME_TYPE_MAP: MutableMap<String, String> = HashMap()

    init {
        VIDEO_FORMAT_MIME_TYPE_MAP["avc"] = "video/avc"
        VIDEO_FORMAT_MIME_TYPE_MAP["hevc"] = "video/hevc"
        VIDEO_FORMAT_MIME_TYPE_MAP["vp9"] = "video/x-vnd.on2.vp9"
        VIDEO_FORMAT_MIME_TYPE_MAP["av1"] = "video/av01"
    }

    val uUID: String
        get() = UUID.randomUUID().toString()

    /**
     * Returns the time in ms since the system was booted, and guaranteed to be monotonic Details
     * here: https://developer.android.com/reference/android/os/SystemClock
     *
     * @return The time in ms since the system was booted, and guaranteed to be monotonic.
     */
    val elapsedTime: Long
        get() = SystemClock.elapsedRealtime()
    val timestamp: Long
        get() = System.currentTimeMillis()
    val locale: String
        get() = Resources.getSystem().configuration.locale.toString()

    val supportedVideoFormats: List<String>
        get() {
            val codecs: MutableList<String> = ArrayList()
            for (format in VIDEO_FORMAT_MIME_TYPE_MAP.keys) {
                if (isMimeTypeSupported(VIDEO_FORMAT_MIME_TYPE_MAP[format])) {
                    codecs.add(format)
                }
            }
            return codecs
        }

    private fun isMimeTypeSupported(mimeType: String?): Boolean {
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (codecInfo.isEncoder) {
                continue
            }

            val types = codecInfo.supportedTypes
            for (type in types) {
                if (type.equals(mimeType, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    fun calculatePercentage(
        numerator: Long?,
        denominator: Long?,
        clamp: Boolean,
    ): Int? {
        if (denominator == null || denominator == 0L || numerator == null) {
            return null
        }
        val result = (((numerator.toFloat() / denominator.toFloat()) * 100)).roundToInt()
        return if (clamp) result.coerceAtMost(100) else result
    }

    fun getHostnameAndPath(uriString: String?): Pair<String?, String?> {
        try {
            val uri = URI(uriString)
            return Pair(uri.host, uri.path)
        } catch (ignored: Exception) {
        }
        return Pair(null, null)
    }

    fun getIsLiveFromConfigOrPlayer(
        isPlayerReady: Boolean,
        isLiveFromConfig: Boolean?,
        isLiveFromPlayer: Boolean,
    ): Boolean {
        return if (isPlayerReady) {
            isLiveFromPlayer
        } else {
            isLiveFromConfig ?: false
        }
    }

    fun isClassLoaded(
        className: String,
        loader: ClassLoader?,
    ): Boolean {
        return try {
            Class.forName(className, false, loader)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    fun toPrimitiveLong(value: Double?): Long {
        return value?.toLong() ?: 0
    }

    fun multiply(
        value: Double?,
        multiplicand: Int?,
    ): Double? {
        return if (value == null || multiplicand == null) null else value * multiplicand
    }

    fun secondsToMillis(seconds: Double?): Long {
        return toPrimitiveLong(multiply(seconds, MILLISECONDS_IN_SECONDS))
    }

    fun joinUrl(
        baseUrl: String,
        relativeUrl: String,
    ): String {
        val result = StringBuilder(baseUrl)
        if (!baseUrl.endsWith("/")) {
            result.append('/')
        }

        var normalizedRelativeUrl = relativeUrl
        if (relativeUrl.startsWith("/")) {
            normalizedRelativeUrl = relativeUrl.substring(1)
        }

        result.append(normalizedRelativeUrl)
        return result.toString()
    }

    fun isTVDevice(context: Context): Boolean {
        val untypedUiModeManager = context.getSystemService(Context.UI_MODE_SERVICE)
        if (untypedUiModeManager is UiModeManager) {
            return untypedUiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        }
        return false
    }

    fun getPlatform(isTV: Boolean): String {
        return if (isTV) "androidTV" else "android"
    }

    fun getDomain(context: Context): String {
        return context.packageName
    }

    val analyticsVersion: String
        get() = BuildConfig.COLLECTOR_CORE_VERSION

    fun getApplicationInfoOrNull(context: Context): ApplicationInfo? {
        var applicationInfo: ApplicationInfo? = null
        try {
            applicationInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getApplicationInfo(
                        context.packageName,
                        PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                    )
                } else {
                    context.packageManager.getApplicationInfo(
                        context.packageName,
                        PackageManager.GET_META_DATA,
                    )
                }
        } catch (e: PackageManager.NameNotFoundException) {
            BitmovinLog.e("Util", "Something went wrong while getting application info, e:", e)
        }
        return applicationInfo
    }

    fun getPackageInfoOrNull(context: Context): PackageInfo? {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.applicationContext.packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                    )
                } else {
                    context.packageManager.getPackageInfo(
                        context.applicationContext.packageName,
                        0,
                    )
                }
        } catch (e: PackageManager.NameNotFoundException) {
            BitmovinLog.e("Util", "Something went wrong while getting package info, e:", e)
        }
        return packageInfo
    }

    private val knownHlsManifestFileExtensions = listOf("m3u8")
    private val knownDashManifestFileExtensions = listOf("mpd")
    private val knownSmoothFileExtensions = listOf("ism", "isml")
    private val knownVideoContainerFileExtensions = listOf("mp4", "m4s", "webm", "mkv", "ts", "mpg", "mpeg", "flv", "ogg")
    private val knownProgressiveFormats =
        listOf("mp4", "m4a", "m4s", "webm", "mkv", "ts", "mpg", "mpeg", "flv", "ogg", "wav", "mp3", "aac", "flac", "amr")

    private val typicalVideoExtensions = listOf("video", "mpeg")

    /**
     * This function set the streamFormat, mpdUrl, m3u8Url, and progUrl in the EventData object based on the file extension of the uri.
     *
     * Note: If the format is unknown or smooth, we will still send the Url as a progressive url.
     * This is a necessary trick as there is no predefined place for the smooth urls in the DataEvent object and we don't want to lose the information.
     */
    fun setEventDataFormatTypeAndUrlBasedOnExtension(
        data: EventData,
        uri: Uri,
    ) {
        // https://www.example/video.mp4?token=1234 -> /video.mp4 -> mp4
        val fileExt = uri.path?.substringAfterLast(".")?.lowercase()

        when (fileExt) {
            in knownHlsManifestFileExtensions -> {
                data.streamFormat = StreamFormat.HLS.value
                data.m3u8Url = uri.toString()
            }

            in knownDashManifestFileExtensions -> {
                data.streamFormat = StreamFormat.DASH.value
                data.mpdUrl = uri.toString()
            }

            in knownSmoothFileExtensions -> {
                data.streamFormat = StreamFormat.SMOOTH.value
                // This is a trick as there no predefined place for the smooth urls in the DataEvent object, but we don't want to loose the information.
                data.progUrl = uri.toString()
            }

            in knownProgressiveFormats -> {
                data.streamFormat = StreamFormat.PROGRESSIVE.value
                data.progUrl = uri.toString()
            }

            else -> {
                /*
                    We don't know the format of the stream, so we don't set the streamFormat.
                    In a last effort, we give the sourcePath as progressive url while not knowing the format.
                    This is a trick as there no predefined place for the raw urls in the DataEvent object, but we don't want to loose the information.
                 */
                data.progUrl = uri.toString()
            }
        }
    }

    /* Best effort.
       We check if the mimeType contains keywords that are usually present in video files.
       Based on:
       https://www.iana.org/assignments/media-types/media-types.xhtml#video
     */
    fun isLikelyVideoMimeType(mimeType: String?): Boolean {
        return typicalVideoExtensions
            .any { mimeType?.startsWith(it) == true }
    }

    fun isLikelyVideoSegment(uri: Uri?): Boolean {
        return uri?.getFileExtension() in knownVideoContainerFileExtensions
    }

    fun isLikelyProgressiveStream(uri: Uri?): Boolean {
        return uri?.getFileExtension() in knownProgressiveFormats
    }
}

fun Uri.getFileExtension(): String? {
    return this.path?.substringAfterLast(".")?.lowercase()
}
