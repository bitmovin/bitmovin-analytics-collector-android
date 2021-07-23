package com.bitmovin.analytics.exoplayer.features

import android.net.Uri
import android.util.Log
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.exoplayer.DefaultAnalyticsListener
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.features.segmenttracking.Segment
import com.bitmovin.analytics.features.segmenttracking.SegmentType
import com.bitmovin.analytics.utils.Util
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.HlsTrackMetadataEntry
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.HttpDataSource
import java.io.IOException

class ExoPlayerSegmentTrackingAdapter(private val player: SimpleExoPlayer, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()
    private val analyticsListener = object : DefaultAnalyticsListener() {
        override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
            val statusCode = loadEventInfo.responseHeaders.responseCode ?: 0
            notifyObservable(eventTime, loadEventInfo, mediaLoadData, true, statusCode)
        }

        override fun onLoadError(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, error: IOException, wasCanceled: Boolean) {
            val statusCode = (error as? HttpDataSource.InvalidResponseCodeException)?.responseCode ?: loadEventInfo.responseHeaders.responseCode ?: 0
            notifyObservable(eventTime, loadEventInfo, mediaLoadData, false, statusCode)
        }
    }

    init {
        wireEvents()
    }

    private fun notifyObservable(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, success: Boolean, statusCode: Int) {
        val segment = mapLoadCompletedArgsToSegment(eventTime, loadEventInfo, mediaLoadData, statusCode, success)
        observableSupport.notify { listener -> listener.onDownloadFinished(OnDownloadFinishedEventObject(segment)) }
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
                if(tokens.size > 1) {
                    val statusCodeString = tokens[1]
                    return statusCodeString.toIntOrNull()
                }
                return null
            }

        // TODO write tests
        private val Map<String, List<String>>.responseCode: Int?
            get() {
                val nullableKeyMap = this as? Map<*, List<String>> ?: return null
                if(nullableKeyMap.contains(null)) {
                    val nullEntryList = nullableKeyMap[null] ?: listOf()
                    if(nullEntryList.isNotEmpty()) {
                        val nullEntry = nullEntryList[0]
                        return nullEntry.extractStatusCode
                    }
                }
                return null
            }

        private fun mapManifestType(uri: Uri, eventTime: AnalyticsListener.EventTime): SegmentType {
            return when (com.google.android.exoplayer2.util.Util.inferContentType(uri)) {
                C.TYPE_DASH -> SegmentType.MANIFEST_DASH
                C.TYPE_HLS -> mapHlsManifestType(uri, eventTime)
                C.TYPE_SS -> SegmentType.MANIFEST_SMOOTH
                else -> SegmentType.MANIFEST
            }
        }

        private fun mapHlsManifestType(uri: Uri, eventTime: AnalyticsListener.EventTime): SegmentType {
            try {
                val window = Timeline.Window()
                // maybe needs currentWindowIndex, currentTimeline
                eventTime.timeline.getWindow(eventTime.windowIndex, window)
                val initialPlaylistUri = window.mediaItem.playbackProperties?.uri
                if (initialPlaylistUri != null) {
                    return if (initialPlaylistUri == uri) SegmentType.MANIFEST_HLS_MASTER else SegmentType.MANIFEST_HLS_VARIANT
                }
            } catch(ignored: Exception) {}
            return SegmentType.MANIFEST_HLS
        }

        private fun mapTrackType(trackType: Int): SegmentType = when (trackType) {
            C.TRACK_TYPE_AUDIO -> SegmentType.MEDIA_AUDIO
            C.TRACK_TYPE_VIDEO,
            C.TRACK_TYPE_DEFAULT -> SegmentType.MEDIA_VIDEO
            C.TRACK_TYPE_TEXT -> SegmentType.MEDIA_SUBTITLES
            else -> SegmentType.UNKNOWN
        }

        private const val HLS_MANIFEST_CLASSNAME ="com.google.android.exoplayer2.source.hls.HlsManifest";
        private val isHlsManifestClassLoaded
            get() = Util.isClassLoaded(HLS_MANIFEST_CLASSNAME, ExoPlayerSegmentTrackingAdapter::class.java.classLoader)

        private fun mapDrmType(eventTime: AnalyticsListener.EventTime): SegmentType {
            if(isHlsManifestClassLoaded) {
                try {
                    val window = Timeline.Window()
                    // maybe needs currentWindowIndex, currentTimeline
                    eventTime.timeline.getWindow(eventTime.windowIndex, window)
                    if(window.manifest is HlsManifest) {
                        return SegmentType.KEY_HLS_AES
                    }
                } catch (ignored: Exception) {
                }
            }
            // TODO SegmentType.DRM_LICENSE_WIDEVINE
            // maybe using trackFormat.drmInitData?.schemeType == "widevine"
            return SegmentType.DRM_OTHER
        }

        private fun mapDataType(eventTime: AnalyticsListener.EventTime, uri:Uri, dataType: Int, trackType: Int, trackFormat: Format?): SegmentType {
            when(dataType) {
                C.DATA_TYPE_DRM -> return mapDrmType(eventTime)
                C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE -> return SegmentType.MEDIA_PROGRESSIVE
                C.DATA_TYPE_MANIFEST -> return mapManifestType(uri, eventTime)
                C.DATA_TYPE_MEDIA,
                C.DATA_TYPE_MEDIA_INITIALIZATION -> return mapTrackType(trackType)
            }
            return SegmentType.UNKNOWN
        }

        private fun mapLoadCompletedArgsToSegment(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, statusCode: Int, success: Boolean): Segment {
            val segmentType = mapDataType(eventTime, loadEventInfo.uri , mediaLoadData.dataType, mediaLoadData.trackType, mediaLoadData.trackFormat)
            return Segment(Util.getTimestamp(), segmentType, loadEventInfo.dataSpec.uri.toString(), loadEventInfo.uri.toString(), statusCode, loadEventInfo.loadDurationMs, null, loadEventInfo.bytesLoaded, success)
        }
    }
}
