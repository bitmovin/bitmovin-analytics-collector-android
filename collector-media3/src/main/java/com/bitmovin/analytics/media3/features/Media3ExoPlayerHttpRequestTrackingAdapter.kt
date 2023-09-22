package com.bitmovin.analytics.media3.features

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.inferContentType
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.hls.HlsManifest
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.httprequesttracking.HttpRequest
import com.bitmovin.analytics.features.httprequesttracking.HttpRequestType
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.media3.Media3Util
import com.bitmovin.analytics.utils.Util
import java.io.IOException

// TODO: we need to get this class under test
@UnstableApi
internal class Media3ExoPlayerHttpRequestTrackingAdapter(private val player: ExoPlayer, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()
    private val analyticsListener = object : AnalyticsListener {

        override fun onLoadCompleted(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData,
        ) {
            catchAndLogException("Exception occurred in onLoadCompleted") {
                val statusCode = loadEventInfo.extractStatusCode ?: 0
                notifyObservable(eventTime, loadEventInfo, mediaLoadData, true, statusCode)
            }
        }

        override fun onLoadError(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData,
            error: IOException,
            wasCanceled: Boolean,
        ) {
            catchAndLogException("Exception occurred in onLoadError") {
                val loadEventInfoStatusCode = loadEventInfo.extractStatusCode ?: 0
                val errorResponseCode =
                    (error as? HttpDataSource.InvalidResponseCodeException)?.responseCode
                notifyObservable(
                    eventTime,
                    loadEventInfo,
                    mediaLoadData,
                    false,
                    errorResponseCode ?: loadEventInfoStatusCode,
                )
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

        // we need to run this on the application thread to prevent exoplayer from crashing
        // when calling the api from a non application thread
        // (this is potentially called from okhttp callback which is on a separate thread)
        Media3Util.executeSyncOrAsyncOnLooperThread(player.applicationLooper) {
            try {
                player.removeAnalyticsListener(analyticsListener)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
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
        private val TAG = Media3ExoPlayerHttpRequestTrackingAdapter::class.java.name

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

        private const val PATTERN = """null=\[(.*?)\]"""
        private val regex = PATTERN.toRegex()

        internal val LoadEventInfo.extractStatusCode: Int?
            get() {
                // the null key contains the status code information
                // this solution is bit hacky since I couldn't find a way to extract the null key from the java hashmap directly
                // using toString and parsing the string works (might be an issue with kotlin/java interoperability)
                // toString returns a string in the following format:
                // {null=[HTTP/1.1 200 OK], Accept-Ranges=[bytes], Access-Control-Allow-Credentials=[false], Access-Control-Allow-Headers=[*], Access-Control-Allow-Methods=[GET,POST,HEAD], ....
                val matchResult = regex.find(this.responseHeaders.toString())
                val statusCodeString = matchResult?.value
                return statusCodeString?.extractStatusCode
            }

        private fun mapManifestType(uri: Uri, eventTime: AnalyticsListener.EventTime): HttpRequestType {
            return when (inferContentType(uri)) {
                C.CONTENT_TYPE_DASH -> HttpRequestType.MANIFEST_DASH
                C.CONTENT_TYPE_HLS -> mapHlsManifestType(uri, eventTime)
                C.CONTENT_TYPE_SS -> HttpRequestType.MANIFEST_SMOOTH
                else -> HttpRequestType.MANIFEST
            }
        }

        private fun mapHlsManifestType(uri: Uri, eventTime: AnalyticsListener.EventTime): HttpRequestType {
            try {
                val window = Timeline.Window()
                // we want the window corresponding to the eventTime that was part of the triggered event
                // thus we use the eventTime.windowIndex and eventTime.timeline
                // (and not eventTime.currentTimeline which corresponds to Player.getCurrentTimeline(),
                // and might not be the same timeline as the one from the eventTime)
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
            C.TRACK_TYPE_DEFAULT,
            -> HttpRequestType.MEDIA_VIDEO
            C.TRACK_TYPE_TEXT -> HttpRequestType.MEDIA_SUBTITLES
            else -> HttpRequestType.UNKNOWN
        }

        private const val HLS_MANIFEST_CLASSNAME = "com.google.android.exoplayer2.source.hls.HlsManifest"
        private val isHlsManifestClassLoaded
            get() = Util.isClassLoaded(HLS_MANIFEST_CLASSNAME, Media3ExoPlayerHttpRequestTrackingAdapter::class.java.classLoader)

        private fun mapDrmType(eventTime: AnalyticsListener.EventTime): HttpRequestType {
            if (isHlsManifestClassLoaded) {
                try {
                    val window = Timeline.Window()
                    // we want the window corresponding to the eventTime that was part of the triggered event
                    // thus we use the eventTime.windowIndex and eventTime.timeline
                    // (and not eventTime.currentTimeline which corresponds to Player.getCurrentTimeline(),
                    // and might not be the same timeline as the one from the eventTime)
                    eventTime.timeline.getWindow(eventTime.windowIndex, window)
                    if (window.manifest is HlsManifest) {
                        return HttpRequestType.KEY_HLS_AES
                    }
                } catch (ignored: Exception) {
                }
            }
            // TODO AN-3302 HttpRequestType.DRM_LICENSE_WIDEVINE
            // maybe using trackFormat.drmInitData?.schemeType == "widevine"
            return HttpRequestType.DRM_OTHER
        }

        private fun mapDataType(eventTime: AnalyticsListener.EventTime, uri: Uri, dataType: Int, trackType: Int): HttpRequestType {
            when (dataType) {
                C.DATA_TYPE_DRM -> return mapDrmType(eventTime)
                C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE -> return HttpRequestType.MEDIA_PROGRESSIVE
                C.DATA_TYPE_MANIFEST -> return mapManifestType(uri, eventTime)
                C.DATA_TYPE_MEDIA,
                C.DATA_TYPE_MEDIA_INITIALIZATION,
                -> return mapTrackType(trackType)
            }
            return HttpRequestType.UNKNOWN
        }

        private fun mapLoadCompletedArgsToHttpRequest(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, statusCode: Int, success: Boolean): HttpRequest {
            val requestType = mapDataType(eventTime, loadEventInfo.uri, mediaLoadData.dataType, mediaLoadData.trackType)
            return HttpRequest(Util.timestamp, requestType, loadEventInfo.dataSpec.uri.toString(), loadEventInfo.uri.toString(), statusCode, loadEventInfo.loadDurationMs, null, loadEventInfo.bytesLoaded, success)
        }
    }
}
