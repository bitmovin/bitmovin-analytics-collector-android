package com.bitmovin.analytics.exoplayer

import android.os.Handler
import android.os.Looper
import com.bitmovin.analytics.utils.Util
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerLibraryInfo
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Player

internal object ExoUtil {

    private const val DASH_MANIFEST_CLASSNAME =
        "com.google.android.exoplayer2.source.dash.manifest.DashManifest"
    private const val HLS_MANIFEST_CLASSNAME =
        "com.google.android.exoplayer2.source.hls.HlsManifest"

    fun exoStateToString(state: Int): String {
        return when (state) {
            Player.STATE_IDLE -> "Idle"
            Player.STATE_BUFFERING -> "Buffering"
            Player.STATE_READY -> "Ready"
            Player.STATE_ENDED -> "Ended"
            else -> "Unknown PlayerState"
        }
    }

    val isDashManifestClassLoaded by lazy {
        Util.isClassLoaded(DASH_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }

    val isHlsManifestClassLoaded by lazy {
        Util.isClassLoaded(HLS_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }

    val playerVersion: String
        get() {
            try {
                // we use reflective access here since otherwise the static version field
                // is compiled into the class and not evaluated during runtime
                // thus if customer is using a different version of exoplayer it will not be
                // taken into account!
                val versionField = ExoPlayerLibraryInfo::class.java.getField("VERSION")
                return versionField[null] as String
            } catch (ignored: NoSuchFieldException) {
            } catch (ignored: IllegalAccessException) {
            } catch (ignored: Exception) {
            }
            return "unknown"
        }

    // Method that can be used to make sure a certain code block is executed
    // on same thread as provided looper
    // Be careful, given code is either executed synchronously when calling
    // thread is same thread as applicationLooper, or asynchronously if not
    // This means code calling this cannot rely on order of execution
    fun executeSyncOrAsyncOnLooperThread(applicationLooper: Looper, function: () -> Unit) {
        if (Thread.currentThread() != applicationLooper.thread) {
            val handler = Handler(applicationLooper)
            handler.post {
                function.invoke()
            }
        } else {
            function.invoke()
        }
    }

    /**
     * Exoplayer organizes audio, video, subtitles, etc.. in track groups.
     * reference: https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/source/TrackGroup.html
     *
     * This method shouldn't be used to determine the currently played audio and video formats since
     * it will return the first selected one in a trackgroup.
     *
     * Each track group has a field that determines if the group is selected, and an array which specifies
     * which tracks in the group are selected specifically. Selected doesn't mean that there can only be
     * one in a track group, for example if all video bitrates are of a track group can be played with the
     * underlying hardware, then all of them will be selected.
     *
     * Example
     * TrackGroup1 -> video with tracks for bitrate 1kb, 2kb, 3kb, etc..
     * TrackGroup2 -> audio english with tracks for stereo, surround, etc..
     * TrackGroup3 -> audio german with tracks for stereo, surround, etc..
     * TrackGroup4 -> subtitles english with 1 track for english
     * TrackGroup5 -> subtitles german with 1 track for german
     */
    fun getActiveSubtitles(player: Player): Format? {
        try {
            if (!player.isCommandAvailable(Player.COMMAND_GET_TRACKS)) {
                return null
            }

            val trackGroups = player.currentTracks.groups
            // subtitles are using C.TRACK_TYPE_TEXT
            val trackGroupsWithTrackType = trackGroups.filter { track -> track.type == C.TRACK_TYPE_TEXT }
            val selectedTrackGroup = trackGroupsWithTrackType.firstOrNull { it.isSelected } ?: return null

            // bit cumbersome to find the actual track in the track group that is selected
            // but I couldn't find a better way
            for (index in 0 until selectedTrackGroup.length) {
                if (selectedTrackGroup.isTrackSelected(index)) {
                    return selectedTrackGroup.getTrackFormat(index)
                }
            }
        } catch (_: Exception) {
        }

        return null
    }
}
