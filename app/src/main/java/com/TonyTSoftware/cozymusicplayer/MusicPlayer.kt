package com.TonyTSoftware.cozymusicplayer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import kotlin.properties.Delegates

class MusicPlayer {
    private var mediaPlayer : MediaPlayer? = null
    private var selectedTrack : Uri? = null
    private var wasPlaying : Boolean = false
    var autoPlay by Delegates.notNull<Boolean>()

    fun getMetaData(context: Context,trackUri : Uri) : Triple<String?,String?, String?> {
        val metaDataRetriever = MediaMetadataRetriever()
        metaDataRetriever.setDataSource(context, trackUri)
        val artist : String? = metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val title : String? = metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val year : String? = metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
        return Triple(title,artist,year)
    }

    fun isStopped(): Boolean {
        return (mediaPlayer == null || selectedTrack == null)
    }

    fun getTrackDuration() : Int {
        return mediaPlayer?.duration ?: -1
    }

    fun getTrackProgress() : Int {
        return mediaPlayer?.currentPosition ?: -1
    }

    fun setTrackProgress(newProgress : Int) {
        mediaPlayer?.seekTo(newProgress)
    }

    fun isPlaying(): Boolean {
        return if (mediaPlayer != null)
            mediaPlayer!!.isPlaying
        else
            false
    }
    fun changeTrack(context : Context, trackUri : Uri?) {
        wasPlaying = false
        if (mediaPlayer != null && (isPlaying() || autoPlay)) {
            stop()
            wasPlaying = true
        }
        if (mediaPlayer == null)
            mediaPlayer = MediaPlayer()
        if (trackUri != null) {
            selectedTrack = trackUri
            mediaPlayer?.setDataSource(context, selectedTrack!!)
        }
        mediaPlayer?.prepare()
        if (autoPlay){
            mediaPlayer!!.setOnCompletionListener {
                MainActivity.mainActivityPtr.nextTrack()
            }
        }
        if (wasPlaying) {
            play()
        }
    }

    fun play() {
        if (selectedTrack != null && !mediaPlayer?.isPlaying!!) {
            if (mediaPlayer == null) return
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