package com.bitmovin.analytics.media3.exoplayer

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Format
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

internal object Media3ExoPlayerUtil {
    fun exoStateToString(state: Int): String {
        return when (state) {
            Player.STATE_IDLE -> "Idle"
            Player.STATE_BUFFERING -> "Buffering"
            Player.STATE_READY -> "Ready"
            Player.STATE_ENDED -> "Ended"
            else -> "Unknown PlayerState"
        }
    }

    val playerVersion: String
        @androidx.annotation.OptIn(UnstableApi::class)
        get() {
            try {
                // we use reflective access here since otherwise the static version field
                // is compiled into the class and not evaluated during runtime
                // thus if customer is using a different version of media3 it will not be
                // taken into account!
                val versionField = MediaLibraryInfo::class.java.getField("VERSION")
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
     * Each track group has a field that determines if the group is selected, and an array which specifies
     * which tracks in the group are selected specifically.
     * TODO: verify which player team why we can have track groups with several tracks being selected for video
     * (can be reproduced with test_live_playWithAutoplay and using IVS_LIVE_1 source)
     *
     * Example
     * TrackGroup1 -> video with tracks for bitrate 1kb, 2kb, 3kb, etc..
     * TrackGroup2 -> audio english with tracks for stereo, surround, etc..
     * TrackGroup3 -> audio german with tracks for stereo, surround, etc..
     * TrackGroup4 -> subtitles english with 1 track for english
     * TrackGroup5 -> subtitles german with 1 track for german
     *
     *
     */
    fun getSelectedFormatFromPlayer(player: Player, trackType: Int): Format? {
        try {
            if (!player.isCommandAvailable(Player.COMMAND_GET_TRACKS)) {
                return null
            }

            val trackGroups = player.currentTracks.groups
            val trackGroupsWithTrackType = trackGroups.filter { track -> track.type == trackType }
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
