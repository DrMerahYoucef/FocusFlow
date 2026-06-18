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
import com.example.FocusFlowApplication
import com.example.data.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RadioPlayerService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val serviceScope = CoroutineScope(Dispatchers.Main)

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

        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .setCallback(object : MediaSession.Callback {})
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stationId = intent?.getStringExtra("station_id")
        val stationName = intent?.getStringExtra("station_name")
        val stationUrl = intent?.getStringExtra("station_url")
        val stationCountry = intent?.getStringExtra("station_country") ?: ""
        val stationDesc = intent?.getStringExtra("station_desc") ?: ""
        val logoUrl = intent?.getStringExtra("station_logo") ?: ""

        if (stationId != null) {
            if (stationUrl != null && stationName != null) {
                // Play directly from parameters (useful for global search previews)
                playStation(
                    RadioStation(
                        id = stationId,
                        name = stationName,
                        country = stationCountry,
                        categoryId = "",
                        streamUrl = stationUrl,
                        logoUrl = logoUrl,
                        description = stationDesc,
                        isCustom = false
                    )
                )
            } else {
                // Fallback to DB
                serviceScope.launch {
                    try {
                        val db = FocusFlowApplication.instance.database
                        val stationsList = db.radioDao().getAllStations().first()
                        val dbStation = stationsList.find { it.id == stationId }
                        if (dbStation != null) {
                            playStation(
                                RadioStation(
                                    id = dbStation.id,
                                    name = dbStation.name,
                                    country = dbStation.country,
                                    categoryId = dbStation.categoryId,
                                    streamUrl = dbStation.streamUrl,
                                    fallbackUrl = dbStation.fallbackUrl,
                                    logoUrl = dbStation.logoUrl,
                                    description = dbStation.description,
                                    isCustom = dbStation.isCustom
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun playStation(station: RadioStation) {
        val item = MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setDisplayTitle(station.name)
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
                putExtra("station_name", station.name)
                putExtra("station_url", station.streamUrl)
                putExtra("station_country", station.country)
                putExtra("station_desc", station.description)
                putExtra("station_logo", station.logoUrl)
            }
            context.startService(intent)
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, RadioPlayerService::class.java))
        }
    }
}
