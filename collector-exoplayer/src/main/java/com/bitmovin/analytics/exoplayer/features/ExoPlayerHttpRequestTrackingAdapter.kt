package com.bitmovin.analytics.exoplayer.features

import android.net.Uri
import android.util.Log
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.httprequesttracking.HttpRequest
import com.bitmovin.analytics.features.httprequesttracking.HttpRequestType
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.utils.Util
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.upstream.HttpDataSource
import java.io.IOException

class ExoPlayerHttpRequestTrackingAdapter(private val player: ExoPlayer, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()
    private val analyticsListener = object : AnalyticsListener {
        override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
            catchAndLogException("Exception occurred in onLoadCompleted") {
                val statusCode = loadEventInfo.responseHeaders.responseCode ?: 0
                notifyObservable(eventTime, loadEventInfo, mediaLoadData, true, statusCode)
            }
        }

        override fun onLoadError(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, error: IOException, wasCanceled: Boolean) {
            catchAndLogException("Exception occurred in onLoadError") {
                val statusCode = (error as? HttpDataSource.InvalidResponseCodeException)?.responseCode ?: loadEventInfo.responseHeaders.responseCode ?: 0
                notifyObservable(eventTime, loadEventInfo, mediaLoadData, false, statusCode)
            }
        }
    }

    init {
        wireEvents()
    }

    private fun notifyObservable(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, success: Boolean, statusCode: Int) {
        val httpRequest = mapLoadCompletedArgsToHttpRequest(eventTime, loadEventInfo, mediaLoadData, statusCode, success)
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
        private val TAG = ExoPlayerHttpRequestTrackingAdapter::class.java.name

        private fun catchAndLogException(msg: String, block: () -> Unit) {
            // As ExoPlayer sometimes has breaking changes, we want to make sure that an optional feature isn't breaking our collector
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, msg, e)
            }
        }

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

        private fun mapManifestType(uri: Uri, eventTime: AnalyticsListener.EventTime): HttpRequestType {
            return when (com.google.android.exoplayer2.util.Util.inferContentType(uri)) {
                C.TYPE_DASH -> HttpRequestType.MANIFEST_DASH
                C.TYPE_HLS -> mapHlsManifestType(uri, eventTime)
                C.TYPE_SS -> HttpRequestType.MANIFEST_SMOOTH
                else -> HttpRequestType.MANIFEST
            }
        }

        private fun mapHlsManifestType(uri: Uri, eventTime: AnalyticsListener.EventTime): HttpRequestType {
            try {
                val window = Timeline.Window()
                // maybe needs currentWindowIndex, currentTimeline
                eventTime.timeline.getWindow(eventTime.windowIndex, window)
                val initialPlaylistUri = window.mediaItem.localConfiguration?.uri
                if (initialPlaylistUri != null) {
                    return if (initialPlaylistUri == uri) HttpRequestType.MANIFEST_HLS_MASTER else HttpRequestType.MANIFEST_HLS_VARIANT
                }
            } catch (ignored: Exception) {}
            return HttpRequestType.MANIFEST_HLS
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

        private fun mapDataType(eventTime: AnalyticsListener.EventTime, uri: Uri, dataType: Int, trackType: Int): HttpRequestType {
            when (dataType) {
                C.DATA_TYPE_DRM -> return mapDrmType(eventTime)
                C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE -> return HttpRequestType.MEDIA_PROGRESSIVE
                C.DATA_TYPE_MANIFEST -> return mapManifestType(uri, eventTime)
                C.DATA_TYPE_MEDIA,
                C.DATA_TYPE_MEDIA_INITIALIZATION -> return mapTrackType(trackType)
            }
            return HttpRequestType.UNKNOWN
        }

        private fun mapLoadCompletedArgsToHttpRequest(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, statusCode: Int, success: Boolean): HttpRequest {
            val requestType = mapDataType(eventTime, loadEventInfo.uri, mediaLoadData.dataType, mediaLoadData.trackType)
            return HttpRequest(Util.getTimestamp(), requestType, loadEventInfo.dataSpec.uri.toString(), loadEventInfo.uri.toString(), statusCode, loadEventInfo.loadDurationMs, null, loadEventInfo.bytesLoaded, success)
        }
    }
}
