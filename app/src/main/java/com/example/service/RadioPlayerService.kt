package com.example.service

import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.data.RadioStation
import com.example.data.StationCatalogue

class RadioPlayerService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)   // pause on headphone unplug
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {})
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stationId = intent?.getStringExtra("station_id")
        if (stationId != null) {
            val station = StationCatalogue.stations.find { it.id == stationId }
            if (station != null) {
                playStation(station)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun playStation(station: RadioStation) {
        val item = MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.country)
                    .setDescription(station.description)
                    .build()
            )
            .build()

        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    companion object {
        fun play(context: Context, station: RadioStation) {
            val intent = Intent(context, RadioPlayerService::class.java).apply {
                putExtra("station_id", station.id)
            }
            context.startService(intent)
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, RadioPlayerService::class.java))
        }
    }
}
