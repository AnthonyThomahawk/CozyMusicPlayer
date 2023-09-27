package com.TonyTSoftware.cozymusicplayer

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat


class MusicService : Service(){
    private val NOTIFICATION_CHANNEL = "MEDIAPLAYER_CHANNEL"
    companion object {
        val musicPlayer = MusicPlayer()
        var trackIndex : Int = -1
        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, MusicService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }
        fun stopService(context: Context) {
            val stopIntent = Intent(context, MusicService::class.java)
            context.stopService(stopIntent)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val input = intent?.getStringExtra("inputExtra")

        val i = intent?.getStringExtra("input")
        if (i == "exit") {
            val mpintent = Intent("MusicServiceIntent")
            mpintent.putExtra("stop", true)
            sendBroadcast(mpintent)
            musicPlayer.stop()
            stopSelf()
        } else if (i == "toggle") {
            val mpintent = Intent("MusicServiceIntent")
            if (!musicPlayer.isPlaying() && !musicPlayer.isStopped()) {
                musicPlayer.play()
                mpintent.putExtra("toggled", "playing")
            } else {
                musicPlayer.pause()
                mpintent.putExtra("toggled", "paused")
            }
            sendBroadcast(mpintent)
        } else if (i == "prev") {
            val mpintent = Intent("MusicServiceIntent")
            mpintent.putExtra("switch", "prev")
            sendBroadcast(mpintent)
        } else if (i == "next") {
            val mpintent = Intent("MusicServiceIntent")
            mpintent.putExtra("switch", "next")
            sendBroadcast(mpintent)
        }

        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val ms = MediaSessionCompat(this, "STAG")

        val exitIntent = Intent(this, MusicService::class.java)
        exitIntent.putExtra("input", "exit")
        val eIntent = PendingIntent.getService(this, 1, exitIntent, PendingIntent.FLAG_MUTABLE)

        val togglePlayBackIntent = Intent(this, MusicService::class.java)
        togglePlayBackIntent.putExtra("input", "toggle")
        val tIntent = PendingIntent.getService(this, 2, togglePlayBackIntent, PendingIntent.FLAG_MUTABLE)

        val nextIntent = Intent(this, MusicService::class.java)
        nextIntent.putExtra("input", "next")
        val nIntent = PendingIntent.getService(this, 4, nextIntent, PendingIntent.FLAG_MUTABLE)

        val prevIntent = Intent(this, MusicService::class.java)
        prevIntent.putExtra("input", "prev")
        val pIntent = PendingIntent.getService(this, 5, prevIntent, PendingIntent.FLAG_MUTABLE)

        val playback : NotificationCompat.Action = if (musicPlayer.isPlaying()) {
            NotificationCompat.Action(R.drawable.ic_media_pause, "Pause", tIntent)
        } else {
            NotificationCompat.Action(R.drawable.ic_media_play, "Play", tIntent)
        }

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setContentTitle("Cozy Music Player")
            .setContentText(input)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_media_play)
            .addAction(R.drawable.ic_media_previous, "Previous", pIntent)
            .addAction(playback)
            .addAction(R.drawable.ic_media_next, "Next", nIntent)
            .addAction(R.drawable.ic_notification_clear_all, "Exit", eIntent)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(1)
                    .setMediaSession(ms.sessionToken)
            )
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(NOTIFICATION_CHANNEL, "MEDIAPLAYER_CHANNEL",
                NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }
}