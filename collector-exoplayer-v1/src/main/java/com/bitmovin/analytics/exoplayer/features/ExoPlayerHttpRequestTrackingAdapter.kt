package com.bitmovin.analytics.exoplayer.features

import android.net.Uri
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.exoplayer.DefaultAnalyticsListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.features.httprequesttracking.HttpRequest
import com.bitmovin.analytics.features.httprequesttracking.HttpRequestType
import com.bitmovin.analytics.utils.Util
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import java.io.IOException

class ExoPlayerHttpRequestTrackingAdapter(private val player: SimpleExoPlayer, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()
    private val analyticsListener = object : DefaultAnalyticsListener() {

        override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: MediaSourceEventListener.LoadEventInfo, mediaLoadData: MediaSourceEventListener.MediaLoadData) {
            val statusCode = loadEventInfo.responseHeaders.responseCode ?: 0
            notifyObservable(eventTime, loadEventInfo, mediaLoadData, true, statusCode)
        }

        override fun onLoadError(eventTime: AnalyticsListener.EventTime, loadEventInfo: MediaSourceEventListener.LoadEventInfo, mediaLoadData: MediaSourceEventListener.MediaLoadData, error: IOException, wasCanceled: Boolean) {
            val statusCode = (error as? HttpDataSource.InvalidResponseCodeException)?.responseCode ?: loadEventInfo.responseHeaders.responseCode ?: 0
            notifyObservable(eventTime, loadEventInfo, mediaLoadData, false, statusCode)
        }
    }

    init {
        wireEvents()
    }

    private fun notifyObservable(eventTime: AnalyticsListener.EventTime, loadEventInfo: MediaSourceEventListener.LoadEventInfo, mediaLoadData: MediaSourceEventListener.MediaLoadData, success: Boolean, statusCode: Int) {
        val httpRequest = mapLoadCompletedArgsToHttpRequest(player, eventTime, loadEventInfo, mediaLoadData, statusCode, success)
        observableSupport.notify { listener -> listener.onDownloadFinished(OnDownloadFinishedEventObject(httpRequest)) }
    }

    private fun wireEvents() {
        onAnalyticsReleasingObservable.subscribe(this)
        player.addAnalyticsListener(analyticsListener)
    }

    private fun unwireEvents() {
        onAnalyticsReleasingObservable.unsubscribe(this)
        player.removeAnalyticsListener(analyticsListener)
    }

    override fun subscribe(listener: OnDownloadFinishedEventListener) {
        observableSupport.subscribe(listener)
    }

    override fun unsubscribe(listener: OnDownloadFinishedEventListener) {
        observableSupport.unsubscribe(listener)
    }

    override fun onReleasing() {
        unwireEvents()
    }

    companion object {
        private val String.extractStatusCode: Int?
            get() {
                val tokens = this.split(' ')
                if (tokens.size > 1) {
                    val statusCodeString = tokens[1]
                    return statusCodeString.toIntOrNull()
                }
                return null
            }

        // TODO write tests
        private val Map<String, List<String>>.responseCode: Int?
            get() {
                val nullableKeyMap = this as? Map<*, List<String>> ?: return null
                if (nullableKeyMap.contains(null)) {
                    val nullEntryList = nullableKeyMap[null] ?: listOf()
                    if (nullEntryList.isNotEmpty()) {
                        val nullEntry = nullEntryList[0]
                        return nullEntry.extractStatusCode
                    }
                }
                return null
            }

        private fun mapManifestType(player: SimpleExoPlayer, uri: Uri, eventTime: AnalyticsListener.EventTime): HttpRequestType {
            return when (com.google.android.exoplayer2.util.Util.inferContentType(uri)) {
                C.TYPE_DASH -> HttpRequestType.MANIFEST_DASH
                C.TYPE_HLS -> mapHlsManifestType(player, uri, eventTime)
                C.TYPE_SS -> HttpRequestType.MANIFEST_SMOOTH
                else -> HttpRequestType.MANIFEST
            }
        }

        private fun mapHlsManifestType(player: SimpleExoPlayer, uri: Uri, eventTime: AnalyticsListener.EventTime): HttpRequestType {
            if (!isHlsManifestClassLoaded) return HttpRequestType.MANIFEST_HLS

            if (!eventTime.timeline.isEmpty) {
                try {

                    val window = Timeline.Window()
                    // maybe needs currentWindowIndex, currentTimeline
                    eventTime.timeline.getWindow(eventTime.windowIndex, window)
                    val manifest = window.manifest as? HlsManifest?
                    if (manifest != null) {
                        return if (manifest.masterPlaylist.baseUri == uri.toString()) HttpRequestType.MANIFEST_HLS_MASTER else HttpRequestType.MANIFEST_HLS_VARIANT
                    }
                } catch (ignored: Exception) {
                }
            }

            // While the source is still being prepared, the timeline is still null
            // so we try to get the current source in preparation, and if it's an HLS
            // source, we check against the base manifest uri
            val mediaSource = player.mediaSourceFromReflection as? HlsMediaSource?
            val manifestUri = mediaSource?.manifestUriFromReflection as? Uri?
            if (manifestUri != null) {
                return if (manifestUri == uri) HttpRequestType.MANIFEST_HLS_MASTER else HttpRequestType.MANIFEST_HLS_VARIANT
            }

            return HttpRequestType.MANIFEST_HLS
        }
        private val HlsMediaSource.manifestUriFromReflection: Any?
            get() {
                return Util.getPrivateFieldFromReflection(this, "manifestUri")
            }

        private val SimpleExoPlayer.mediaSourceFromReflection: Any?
            get() {
                return Util.getPrivateFieldFromReflection(this, "mediaSource")
            }

        private fun mapTrackType(trackType: Int): HttpRequestType = when (trackType) {
            C.TRACK_TYPE_AUDIO -> HttpRequestType.MEDIA_AUDIO
            C.TRACK_TYPE_VIDEO,
            C.TRACK_TYPE_DEFAULT -> HttpRequestType.MEDIA_VIDEO
            C.TRACK_TYPE_TEXT -> HttpRequestType.MEDIA_SUBTITLES
            else -> HttpRequestType.UNKNOWN
        }

        private const val HLS_MANIFEST_CLASSNAME = "com.google.android.exoplayer2.source.hls.HlsManifest"
        private val isHlsManifestClassLoaded
            get() = Util.isClassLoaded(HLS_MANIFEST_CLASSNAME, ExoPlayerHttpRequestTrackingAdapter::class.java.classLoader)

        private fun mapDrmType(eventTime: AnalyticsListener.EventTime): HttpRequestType {
            if (isHlsManifestClassLoaded) {
                try {
                    val window = Timeline.Window()
                    // maybe needs currentWindowIndex, currentTimeline
                    eventTime.timeline.getWindow(eventTime.windowIndex, window)
                    if (window.manifest is HlsManifest) {
                        return HttpRequestType.KEY_HLS_AES
                    }
                } catch (ignored: Exception) {
                }
            }
            // TODO HttpRequestType.DRM_LICENSE_WIDEVINE
            // maybe using trackFormat.drmInitData?.schemeType == "widevine"
            return HttpRequestType.DRM_OTHER
        }

        private fun mapDataType(player: SimpleExoPlayer, eventTime: AnalyticsListener.EventTime, uri: Uri, dataType: Int, trackType: Int, trackFormat: Format?): HttpRequestType {
            when (dataType) {
                C.DATA_TYPE_DRM -> return mapDrmType(eventTime)
                C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE -> return HttpRequestType.MEDIA_PROGRESSIVE
                C.DATA_TYPE_MANIFEST -> return mapManifestType(player, uri, eventTime)
                C.DATA_TYPE_MEDIA,
                C.DATA_TYPE_MEDIA_INITIALIZATION -> return mapTrackType(trackType)
            }
            return HttpRequestType.UNKNOWN
        }

        private fun mapLoadCompletedArgsToHttpRequest(player: SimpleExoPlayer, eventTime: AnalyticsListener.EventTime, loadEventInfo: MediaSourceEventListener.LoadEventInfo, mediaLoadData: MediaSourceEventListener.MediaLoadData, statusCode: Int, success: Boolean): HttpRequest {
            val requestType = mapDataType(player, eventTime, loadEventInfo.uri, mediaLoadData.dataType, mediaLoadData.trackType, mediaLoadData.trackFormat)
            return HttpRequest(Util.getTimestamp(), requestType, loadEventInfo.dataSpec.uri.toString(), loadEventInfo.uri.toString(), statusCode, loadEventInfo.loadDurationMs, null, loadEventInfo.bytesLoaded, success)
        }
    }
}
