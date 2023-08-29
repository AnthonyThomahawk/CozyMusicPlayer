package com.TonyTSoftware.cozymusicplayer

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton


class MainActivity : AppCompatActivity() {
    private val folderPickRequestCode = 100
    private var audioFilesUri : ArrayList<Uri> = ArrayList()
    private lateinit var tracksLoadedView : TextView
    private lateinit var playbackBtn : Button
    private lateinit var stopBtn : Button
    private lateinit var currentTrackView: TextView
    private lateinit var musicPlayer: MusicPlayer
    private lateinit var trackList : ArrayList<ListItemData>

    companion object { // a bit non optimal, will change this later
        lateinit var mainActivityPtr : MainActivity
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityPtr = this

        musicPlayer = MusicPlayer()
        currentTrackView = findViewById(R.id.textView3)
        playbackBtn = findViewById(R.id.playbackBtn)
        stopBtn = findViewById(R.id.stopBtn)

        playbackBtn.setOnClickListener {
            toggleMusicPlayback()
        }

        stopBtn.setOnClickListener {
            stopPlayback()
        }

        val selectFolderBtn : Button = findViewById(R.id.selectfolderbtn)

        tracksLoadedView = findViewById(R.id.textView2)
        tracksLoadedView.text = audioFilesUri.size.toString() + " tracks loaded"

        selectFolderBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(Intent.createChooser(intent, "Choose directory"), folderPickRequestCode);
        }
    }

    fun selectTrack(trackIndex : Int) {
        musicPlayer.changeTrack(applicationContext, audioFilesUri[trackIndex])
        currentTrackView.text = trackList[trackIndex].getFileName()
    }

    private fun stopPlayback() {
        if (!musicPlayer.isStopped()) {
            musicPlayer.stop()
            currentTrackView.text = "No track selected"
            playbackBtn.text = "Play"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_play)
        }
    }

    private fun toggleMusicPlayback() {
        if (!musicPlayer.isPlaying() && !musicPlayer.isStopped()) {
            musicPlayer.play()
            playbackBtn.text = "Pause"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause)
        } else {
            musicPlayer.pause()
            playbackBtn.text = "Play"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_play)
        }
    }

    private fun refreshUI() {
        trackList = ArrayList()
        for ((index, audioFileUri) in audioFilesUri.withIndex()) {
            val audioFileDoc : DocumentFile? = DocumentFile.fromSingleUri(this, audioFileUri)
            val filename = audioFileDoc?.name
            val metaDataRetriever = MediaMetadataRetriever()
            metaDataRetriever.setDataSource(this, audioFileUri)
            val artist : String? = metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val title : String? = metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            trackList.add(ListItemData(filename, title, artist, index))
        }
        tracksLoadedView.text = audioFilesUri.size.toString() + " tracks loaded"
        val recyclerView : RecyclerView = findViewById(R.id.recyclerView)
        val trackListAdapter = TrackListAdapter(trackList)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = trackListAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == folderPickRequestCode) {
            val selectedFolderUri = data?.data
            if (selectedFolderUri != null) {
                val selectedFolder : DocumentFile? = DocumentFile.fromTreeUri(this, selectedFolderUri)
                val filesInFolder = selectedFolder?.listFiles()
                if (filesInFolder != null)
                    for (file in filesInFolder)
                        if (file.isFile) {
                            val musicFileExtensions = arrayOf("mp3", "ogg", "flac", "m4a", "3gp", "wav")
                            val fileExtension = file.name?.substringAfterLast('.', "")
                            if (fileExtension in musicFileExtensions)
                                audioFilesUri.add(file.uri)
                        }
                refreshUI()
            }
        }
    }

}