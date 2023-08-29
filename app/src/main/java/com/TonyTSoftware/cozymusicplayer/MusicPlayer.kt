package com.TonyTSoftware.cozymusicplayer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class MusicPlayer {
    private var mediaPlayer : MediaPlayer? = null
    private var selectedTrack : Uri? = null
    private var wasPlaying : Boolean = false

    fun isStopped(): Boolean {
        return (mediaPlayer == null || selectedTrack == null)
    }

    fun isPlaying(): Boolean {
        return if (mediaPlayer != null)
            mediaPlayer!!.isPlaying
        else
            false
    }
    fun changeTrack(context : Context, trackUri : Uri?) {
        if (mediaPlayer != null && isPlaying()) {
            stop()
            wasPlaying = true
        }
        mediaPlayer = MediaPlayer()
        if (trackUri != null) {
            selectedTrack = trackUri
            mediaPlayer?.setDataSource(context, selectedTrack!!)
        }
        mediaPlayer?.prepare()
        if (wasPlaying) {
            play()
        }
    }

    fun play() {
        if (selectedTrack != null && !mediaPlayer?.isPlaying!!) {
            mediaPlayer?.start()
        }
    }

    fun pause() {
        if (selectedTrack != null && mediaPlayer?.isPlaying!!) {
            mediaPlayer?.pause()
        }
    }

    fun stop() {
        if (selectedTrack != null) {
            selectedTrack = null
            mediaPlayer?.release()
            mediaPlayer = null
            wasPlaying = false
        }
    }
}