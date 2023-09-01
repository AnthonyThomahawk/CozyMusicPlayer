package com.TonyTSoftware.cozymusicplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
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
    private lateinit var prevBtn : Button
    private lateinit var nextBtn : Button
    private lateinit var currentTrackView: TextView
    private lateinit var musicPlayer: MusicPlayer
    private lateinit var trackList : ArrayList<ListItemData>
    private var currentTrackIndex : Int? = -1
    private lateinit var seekBarThread : Thread
    private lateinit var seekBar : SeekBar
    private var seekBarThreadRunning : Boolean = false
    private var seekBarisHeld : Boolean = false
    private lateinit var timeView : TextView

    companion object { // a bit non optimal, will change this later
        lateinit var mainActivityPtr : MainActivity
        lateinit var mainActivityCont : Context
    }
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityPtr = this
        mainActivityCont = this

        musicPlayer = MusicPlayer()
        currentTrackView = findViewById(R.id.textView3)
        playbackBtn = findViewById(R.id.playbackBtn)
        stopBtn = findViewById(R.id.stopBtn)
        prevBtn = findViewById(R.id.prevBtn)
        nextBtn = findViewById(R.id.nextBtn)
        seekBar = findViewById(R.id.seekBar)
        timeView = findViewById(R.id.timeView)

        seekBar.max = 1
        seekBar.progress = 0
        seekBar.isEnabled = false

        val test = this.theme

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // nothing for now
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if (seekBarThreadRunning) {
                    seekBarisHeld = true
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekBarThreadRunning) {
                    musicPlayer.setTrackProgress(seekBar.progress)
                    seekBarisHeld = false
                }
            }
        })

        playbackBtn.setOnClickListener {
            toggleMusicPlayback()
        }

        stopBtn.setOnClickListener {
            stopPlayback()
        }

        prevBtn.setOnClickListener {
            prevTrack()
        }

        nextBtn.setOnClickListener {
            nextTrack()
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

    private fun getMinutesSecondsString(totalTimeMs : Int) : Pair<String,String>{
        val totalSeconds = totalTimeMs / 1000
        val finalMinutes = totalSeconds / 60
        val finalSeconds = totalSeconds % 60

        val finalMinutesString : String = if (finalMinutes >= 10) finalMinutes.toString() else "0$finalMinutes"
        val finalSecondsString : String = if (finalSeconds >= 10) finalSeconds.toString() else "0$finalSeconds"

        return Pair(finalMinutesString, finalSecondsString)
    }

    @SuppressLint("SetTextI18n")
    fun selectTrack(trackIndex : Int) {
        if (seekBarThreadRunning)
        {
            seekBarThreadRunning = false
            seekBarisHeld = false
        }

        currentTrackIndex = trackIndex
        musicPlayer.changeTrack(this, audioFilesUri[trackIndex])
        val (title, artist) = musicPlayer.getMetaData(this, audioFilesUri[trackIndex])
        if (title == null || artist == null)
            currentTrackView.text = trackList[trackIndex].getFileName() + "\n" + "No metadata"
        else
            currentTrackView.text = "Title : $title\nArtist : $artist"

        seekBarThreadRunning = true
        seekBarisHeld = false
        seekBar.isEnabled = true

        seekBarThread = Thread {

            while (true) {
                if (!seekBarThreadRunning)
                    break

                while (seekBarisHeld) {
                    val total = musicPlayer.getTrackDuration()
                    val trackProgress = musicPlayer.getTrackProgress()

                    if (total != -1 && trackProgress != -1) {
                        val (currentMinutesString, currentSecondsString) = getMinutesSecondsString(trackProgress)
                        val (finalMinutesString, finalSecondsString) = getMinutesSecondsString(total)
                        val (seekMinutesString, seekSecondsString) = getMinutesSecondsString(seekBar.progress)

                        runOnUiThread {
                            timeView.text = "$currentMinutesString:$currentSecondsString / $finalMinutesString:$finalSecondsString  Seeking to ( $seekMinutesString:$seekSecondsString )"
                        }
                        Thread.sleep(10)
                    }
                }

                val total = musicPlayer.getTrackDuration()
                val trackProgress = musicPlayer.getTrackProgress()

                if (total != -1 && trackProgress != -1) {
                    seekBar.max = total
                    seekBar.progress = trackProgress

                    val (currentMinutesString, currentSecondsString) = getMinutesSecondsString(trackProgress)
                    val (finalMinutesString, finalSecondsString) = getMinutesSecondsString(total)

                    runOnUiThread {
                        timeView.text = "$currentMinutesString:$currentSecondsString / $finalMinutesString:$finalSecondsString"
                    }
                }

                Thread.sleep(10)
            }
        }

        if (!seekBarThread.isAlive)
            seekBarThread.start()
    }

    @SuppressLint("SetTextI18n")
    private fun stopPlayback() {
        if (!musicPlayer.isStopped()) {
            seekBarThreadRunning = false
            seekBar.max = 1
            seekBar.progress = 0
            seekBar.isEnabled = false
            timeView.text = "00:00 / 00:00"
            musicPlayer.stop()
            currentTrackView.text = "No track selected"
            playbackBtn.text = "Play"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_play)
        }
    }

    @SuppressLint("SetTextI18n")
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

    private fun nextTrack() {
        if (currentTrackIndex != -1) {
            currentTrackIndex = if (currentTrackIndex == audioFilesUri.size - 1)
                0
            else
                currentTrackIndex!! + 1
            selectTrack(currentTrackIndex!!)
        }
    }

    private fun prevTrack() {
        if (currentTrackIndex != -1) {
            currentTrackIndex = if (currentTrackIndex == 0)
                audioFilesUri.size - 1
            else
                currentTrackIndex!! - 1
            selectTrack(currentTrackIndex!!)
        }
    }



    @SuppressLint("SetTextI18n")
    private fun refreshUI() {
        trackList = ArrayList()
        for ((index, audioFileUri) in audioFilesUri.withIndex()) {
            val audioFileDoc : DocumentFile? = DocumentFile.fromSingleUri(this, audioFileUri)
            val filename = audioFileDoc?.name
            val (title, artist) = musicPlayer.getMetaData(this, audioFileUri)
            trackList.add(ListItemData(filename, title, artist, index))
        }
        val recyclerView : RecyclerView = findViewById(R.id.recyclerView)
        val trackListAdapter = TrackListAdapter(trackList)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = trackListAdapter
        tracksLoadedView.text = trackListAdapter.itemCount.toString() + " tracks loaded"
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
                            if (fileExtension in musicFileExtensions && file.uri !in audioFilesUri)
                                audioFilesUri.add(file.uri)
                        }
                refreshUI()
            }
        }
    }

}