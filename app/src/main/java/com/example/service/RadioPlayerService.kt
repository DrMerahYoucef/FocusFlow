package com.example.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommands
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.LibraryResult
import com.google.common.collect.ImmutableList
import com.example.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class RadioPlayerService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private val firestore by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
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
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let {
                    saveLastPlayedMediaItem(it)
                }
            }
        })

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionCallback = object : MediaLibrarySession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                return super.onConnect(session, controller)
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId("ROOT_ID")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val lastItem = getLastPlayedMediaItem()
                return if (lastItem != null && lastItem.mediaId == mediaId) {
                    Futures.immediateFuture(LibraryResult.ofItem(lastItem, null))
                } else {
                    Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                }
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val lastItem = getLastPlayedMediaItem()
                val items = if (lastItem != null) {
                    ImmutableList.of(lastItem)
                } else {
                    ImmutableList.of()
                }
                return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
            }

            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val lastItem = getLastPlayedMediaItem()
                val items = if (lastItem != null) listOf(lastItem) else emptyList()
                val mediaItemsWithStartPosition = MediaSession.MediaItemsWithStartPosition(
                    items,
                    0,
                    0L
                )
                return Futures.immediateFuture(mediaItemsWithStartPosition)
            }
        }

        mediaSession = MediaLibrarySession.Builder(this, player, sessionCallback)
            .setSessionActivity(pendingIntent)
            .build()
    }

    private fun saveLastPlayedMediaItem(mediaItem: MediaItem) {
        val prefs = getSharedPreferences("RadioPlayerPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("last_media_id", mediaItem.mediaId)
            putString("last_stream_url", mediaItem.localConfiguration?.uri?.toString())
            putString("last_title", mediaItem.mediaMetadata.title?.toString())
            putString("last_artist", mediaItem.mediaMetadata.artist?.toString())
            putString("last_description", mediaItem.mediaMetadata.description?.toString())
            putString("last_artwork_uri", mediaItem.mediaMetadata.artworkUri?.toString())
            apply()
        }
    }

    private fun getLastPlayedMediaItem(): MediaItem? {
        val prefs = getSharedPreferences("RadioPlayerPrefs", Context.MODE_PRIVATE)
        val mediaId = prefs.getString("last_media_id", null) ?: return null
        val streamUrl = prefs.getString("last_stream_url", null) ?: return null
        val title = prefs.getString("last_title", null)
        val artist = prefs.getString("last_artist", null)
        val description = prefs.getString("last_description", null)
        val artworkUriStr = prefs.getString("last_artwork_uri", null)

        val metadataBuilder = MediaMetadata.Builder()
        if (title != null) metadataBuilder.setTitle(title)
        if (artist != null) metadataBuilder.setArtist(artist)
        if (description != null) metadataBuilder.setDescription(description)
        if (artworkUriStr != null) {
            try {
                metadataBuilder.setArtworkUri(Uri.parse(artworkUriStr))
            } catch (e: Exception) {
                // ignore
            }
        }

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(streamUrl)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun updateRadioStatus(stationName: String) {
        try {
            val uid = auth.currentUser?.uid ?: return
            firestore.collection("users").document(uid)
                .update("currentRadio", stationName)
                .addOnFailureListener { /* silent fail — not critical */ }
        } catch (e: Exception) {
            android.util.Log.e("RadioPlayerService", "Failed to update radio status on Firestore (offline/uninitialized)", e)
        }
    }

    // This is the key method — Media3 calls this to get the session
    // and automatically builds the notification + lock screen controls
    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaLibrarySession? = mediaSession

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
