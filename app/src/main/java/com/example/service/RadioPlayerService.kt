package com.example.service

import android.content.Context
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class RadioPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val firestore by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }

    override fun onCreate() {
        super.onCreate()

        val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            createAttributionContext("media")
        } else {
            this
        }

        val player = ExoPlayer.Builder(attributionContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    val title = player.mediaMetadata.title?.toString() ?: ""
                    updateRadioStatus(title)
                } else {
                    updateRadioStatus("")
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE ||
                    playbackState == Player.STATE_ENDED) {
                    updateRadioStatus("")
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun updateRadioStatus(stationName: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("currentRadio", stationName)
            .addOnFailureListener { /* silent fail — not critical */ }
    }

    // This is the key method — Media3 calls this to get the session
    // and automatically builds the notification + lock screen controls
    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        // Clear station status on destroy
        updateRadioStatus("")
        
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
