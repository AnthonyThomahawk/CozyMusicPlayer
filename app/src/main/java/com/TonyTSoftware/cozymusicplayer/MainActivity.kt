package com.TonyTSoftware.cozymusicplayer

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
    private val folderPickRequestCode = 100
    private var audioFilesUri : ArrayList<Uri> = ArrayList()
    private lateinit var tracksLoadedView : TextView
    private lateinit var playbackBtn : Button
    private lateinit var stopBtn : Button
    private lateinit var prevBtn : Button
    private lateinit var nextBtn : Button
    private lateinit var currentTrackView: TextView
    private lateinit var trackList : ArrayList<ListItemData>
    private var currentTrackIndex : Int? = -1

    private lateinit var seekBar : SeekBar

    private var seekBarThreadRunning : Boolean = false
    private var seekBarisHeld : Boolean = false
    private lateinit var timeView : TextView
    private lateinit var autoPlaySwitch : Switch
    private lateinit var shuffleSwitch : Switch
    private var shuffle : Boolean = false
    private val PREFS_NAME = "MUSICPLAYER_SETTINGS"

    companion object {
        lateinit var mainActivityPtr : MainActivity
        var threadsRunning by Delegates.notNull<Int>()
    }
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkSelfPermission("android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_DENIED) {
            val alertDialog: androidx.appcompat.app.AlertDialog = androidx.appcompat.app.AlertDialog.Builder(this@MainActivity).create()
            alertDialog.setTitle("Phone permission request")
            alertDialog.setMessage("The phone permission is needed for auto-pause of playback when receiving a call." +
                    " This app is open-source and does not record your calls or use the permission for malicious purposes." +
                    "  Please allow it in the next dialog.")
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL, "OK"
            ) { dialog, _ ->
                run {
                    requestPermissions(arrayOf("android.permission.READ_PHONE_STATE"), 1)
                    dialog.dismiss()
                }
            }
            alertDialog.show()
        }

        threadsRunning = 0

        mainActivityPtr = this

        currentTrackView = findViewById(R.id.textView3)
        playbackBtn = findViewById(R.id.playbackBtn)
        stopBtn = findViewById(R.id.stopBtn)
        prevBtn = findViewById(R.id.prevBtn)
        nextBtn = findViewById(R.id.nextBtn)
        seekBar = findViewById(R.id.seekBar)
        timeView = findViewById(R.id.timeView)
        autoPlaySwitch = findViewById(R.id.switch1)
        shuffleSwitch = findViewById(R.id.switch2)

        seekBar.max = 1
        seekBar.progress = 0
        seekBar.isEnabled = false
        seekBarThreadRunning = false

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
                    MusicService.musicPlayer.setTrackProgress(seekBar.progress)
                    seekBarisHeld = false
                }
            }
        })

        val mediaControlsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(cont: Context, i: Intent) {
                val extras = i.extras
                if (extras != null) {
                    if (extras.getString("toggled") != null) {
                        if (extras.getString("toggled") == "paused") {
                            playbackBtn.text = "Play"
                            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(mainActivityPtr.baseContext, android.R.drawable.ic_media_play)
                        }
                        else {
                            playbackBtn.text = "Pause"
                            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(mainActivityPtr.baseContext, android.R.drawable.ic_media_pause)
                        }
                    }
                    if (extras.getBoolean("stop")) {
                        seekBarThreadRunning = false
                        seekBar.isEnabled = false
                        currentTrackView.text = "No track selected"
                        playbackBtn.text = "Play"
                        (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(
                            mainActivityPtr.baseContext, android.R.drawable.ic_media_play)
                    }
                    if (extras.getString("switch") != null) {
                        if (extras.getString("switch") == "prev") {
                            prevTrack()
                        }
                        else {
                            nextTrack()
                        }
                    }
                }
            }
        }

        if (!MusicService.mediaReceiverInit) { // check for duplicate receiver on new activity
            MusicService.mediaReceiver = mediaControlsReceiver
            MusicService.mediaReceiverInit = true
        }

        // register the old receiver if needed

        registerReceiver(MusicService.mediaReceiver, IntentFilter("MusicServiceIntent"))

        val selectFolderBtn : Button = findViewById(R.id.selectfolderbtn)

        tracksLoadedView = findViewById(R.id.textView2)
        tracksLoadedView.text = audioFilesUri.size.toString() + " tracks loaded"

        selectFolderBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(Intent.createChooser(intent, "Choose directory"), folderPickRequestCode);
        }

        val settings = getSharedPreferences(PREFS_NAME, 0)


        val retrievedPaths = settings.getStringSet("stringUris", null)

        if (retrievedPaths != null) {
            for (strUri in retrievedPaths) {
                audioFilesUri.add(strUri.toUri())
            }
            refreshUI()
        }

        playbackBtn.setOnClickListener {
            if (!isMusicServiceRunning())
                MusicService.startService(this)
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

        MusicService.musicPlayer.autoPlay = autoPlaySwitch.isChecked

        autoPlaySwitch.setOnClickListener {
            MusicService.musicPlayer.autoPlay = autoPlaySwitch.isChecked
        }

        shuffleSwitch.setOnClickListener {
            if (shuffleSwitch.isChecked) {
                MusicService.musicPlayer.autoPlay = true
                autoPlaySwitch.isEnabled = false
                autoPlaySwitch.isChecked = true
                shuffle = true
            }
            else {
                autoPlaySwitch.isEnabled = true
                shuffle = false
            }
        }

        if (isMusicServiceRunning()) {
            loadDataFromService()
        }
    }

    private fun loadDataFromService() {
        if (seekBarThreadRunning) {
            seekBarThreadRunning = false
            Thread.sleep(20)
        }

        val (title, artist) = MusicService.musicPlayer.getMetaData(this, audioFilesUri[MusicService.trackIndex])
        if (title == null || artist == null)
            currentTrackView.text = trackList[MusicService.trackIndex].getFileName() + "\n" + "No metadata"
        else
            currentTrackView.text = "Title : $title\nArtist : $artist"

        if (MusicService.musicPlayer.isPlaying() && !MusicService.musicPlayer.isStopped()) {
            playbackBtn.text = "Pause"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(mainActivityPtr.baseContext, android.R.drawable.ic_media_pause)
        } else {
            playbackBtn.text = "Play"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(mainActivityPtr.baseContext, android.R.drawable.ic_media_play)
        }

        currentTrackIndex = MusicService.trackIndex

        seekBarisHeld = false
        seekBar.isEnabled = true

        val seekBarThread = Thread {
            if (threadsRunning != 0) {
                return@Thread
            }
            threadsRunning = threadsRunning++
            while (true) {
                if (!seekBarThreadRunning || MusicService.musicPlayer.isStopped()) {
                    runOnUiThread {
                        timeView.text = "00:00 / 00:00"
                        seekBar.max = 1
                        seekBar.progress = 0
                    }
                    threadsRunning = threadsRunning--
                    break
                }

                while (seekBarisHeld) {
                    val total = MusicService.musicPlayer.getTrackDuration()
                    val trackProgress = MusicService.musicPlayer.getTrackProgress()

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

                if (threadsRunning != 0) {
                    return@Thread
                }

                val total = MusicService.musicPlayer.getTrackDuration()
                val trackProgress = MusicService.musicPlayer.getTrackProgress()

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

        if (!seekBarThreadRunning) {
            seekBarThreadRunning = true
            seekBarThread.start()
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
        if (seekBarThreadRunning) {
            seekBarThreadRunning = false
            Thread.sleep(20)
        }

        MusicService.startService(this)

        currentTrackIndex = trackIndex
        MusicService.trackIndex = trackIndex
        MusicService.musicPlayer.changeTrack(this, audioFilesUri[trackIndex])
        val (title, artist) = MusicService.musicPlayer.getMetaData(this, audioFilesUri[trackIndex])
        if (title == null || artist == null)
            currentTrackView.text = trackList[trackIndex].getFileName() + "\n" + "No metadata"
        else
            currentTrackView.text = "Title : $title\nArtist : $artist"


        seekBarisHeld = false
        seekBar.isEnabled = true

        val seekBarThread = Thread {
            if (threadsRunning != 0) {
                return@Thread
            }
            threadsRunning = threadsRunning++
            while (true) {
                if (!seekBarThreadRunning || MusicService.musicPlayer.isStopped()) {
                    runOnUiThread {
                        timeView.text = "00:00 / 00:00"
                        seekBar.max = 1
                        seekBar.progress = 0
                    }
                    threadsRunning = threadsRunning--
                    break
                }

                while (seekBarisHeld) {
                    val total = MusicService.musicPlayer.getTrackDuration()
                    val trackProgress = MusicService.musicPlayer.getTrackProgress()

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

                if (threadsRunning != 0) {
                    return@Thread
                }

                val total = MusicService.musicPlayer.getTrackDuration()
                val trackProgress = MusicService.musicPlayer.getTrackProgress()

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

        if (!seekBarThreadRunning) {
            seekBarThreadRunning = true
            seekBarThread.start()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun stopPlayback() {
        if (!MusicService.musicPlayer.isStopped()) {
            seekBarThreadRunning = false
            seekBar.isEnabled = false
            if (isMusicServiceRunning())
                MusicService.musicPlayer.stop()
            MusicService.stopService(this)
            currentTrackView.text = "No track selected"
            playbackBtn.text = "Play"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_play)
        }

    }

    @SuppressLint("SetTextI18n")
    private fun toggleMusicPlayback() {
        if (!MusicService.musicPlayer.isPlaying() && !MusicService.musicPlayer.isStopped()) {
            MusicService.musicPlayer.play()
            playbackBtn.text = "Pause"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause)
        } else {
            MusicService.musicPlayer.pause()
            playbackBtn.text = "Play"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_play)
        }
    }

    fun nextTrack() {
        if (shuffle) {
            currentTrackIndex = (0..<audioFilesUri.size).shuffled().last()
            selectTrack(currentTrackIndex!!)
            return
        }
        if (currentTrackIndex != -1) {
            currentTrackIndex = if (currentTrackIndex == audioFilesUri.size - 1)
                0
            else
                currentTrackIndex!! + 1
            selectTrack(currentTrackIndex!!)
        }
    }

    fun prevTrack() {
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
            val (title, artist) = MusicService.musicPlayer.getMetaData(this, audioFileUri)
            trackList.add(ListItemData(filename, title, artist, index))
        }
        val recyclerView : RecyclerView = findViewById(R.id.recyclerView)
        val trackListAdapter = TrackListAdapter(trackList)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = trackListAdapter
        tracksLoadedView.text = trackListAdapter.itemCount.toString() + " tracks loaded"
    }

    private fun scanFolderUri(selectedFolderUri : Uri?) {
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
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == folderPickRequestCode) {
            val contentResolver = applicationContext.contentResolver

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            data?.data?.let { contentResolver.takePersistableUriPermission(it, takeFlags) }


            scanFolderUri(data?.data)

            val settings = getSharedPreferences(PREFS_NAME, 0)
            val editor = settings.edit()

            val retrievedPaths = settings.getStringSet("stringUris", null)

            lateinit var stringUriSet : MutableSet<String>
            var startIndex = 0

            if (retrievedPaths != null) {
                stringUriSet = retrievedPaths
                editor.remove("stringUris")
            } else {
                stringUriSet = mutableSetOf(audioFilesUri[0].toString())
                startIndex = 1
            }

            for (i in startIndex..<audioFilesUri.size) {
                val uri = audioFilesUri[i]
                val uriStr = uri.toString()
                if (uriStr !in stringUriSet) {
                    stringUriSet.add(uriStr)
                }
            }

            editor.putStringSet("stringUris", stringUriSet)

            editor.commit()

            refreshUI()
        }
    }

    override fun onDestroy() {
        seekBarThreadRunning = false
        super.onDestroy()
    }

    private fun isMusicServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (MusicService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}