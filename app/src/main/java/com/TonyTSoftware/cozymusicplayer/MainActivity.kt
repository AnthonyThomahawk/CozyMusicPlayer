package com.TonyTSoftware.cozymusicplayer

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.Arrays
import java.util.Date
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
    private val folderPickRequestCode = 100
    private var audioFilesUri : ArrayList<Uri> = ArrayList()
    private var foldersAddedUri : ArrayList<Uri> = ArrayList()
    private lateinit var tracksLoadedView : TextView
    private lateinit var playbackBtn : Button
    private lateinit var stopBtn : Button
    private lateinit var prevBtn : Button
    private lateinit var nextBtn : Button
    private lateinit var currentTrackView: TextView
    private lateinit var trackList : ArrayList<ListItemData>
    private lateinit var recyclerView : RecyclerView
    var currentTrackIndex : Int? = -1
    private var actFirstLaunch = true

    private lateinit var seekBar : SeekBar

    private var seekBarThreadRunning : Boolean = false
    private var seekBarisHeld : Boolean = false
    private lateinit var timeView : TextView
    private lateinit var autoPlaySwitch : Switch
    private val PREFS_NAME = "MUSICPLAYER_SETTINGS"

    companion object {
        lateinit var mainActivityPtr : MainActivity
        var threadsRunning by Delegates.notNull<Int>()
    }
    // Thread that updates seekbar
    private fun createSeekBarThread() : Thread {
        return Thread {
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

                val total: Int = MusicService.musicPlayer.getTrackDuration()
                val trackProgress: Int = MusicService.musicPlayer.getTrackProgress()

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
    }

    @SuppressLint("SetTextI18n", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // number of seekbar threads running
        threadsRunning = 0

        // pointer to main activity context
        mainActivityPtr = this

        // find UI elements by ID
        val selectFolderBtn : Button = findViewById(R.id.selectfolderbtn)
        currentTrackView = findViewById(R.id.textView3)
        playbackBtn = findViewById(R.id.playbackBtn)
        stopBtn = findViewById(R.id.stopBtn)
        prevBtn = findViewById(R.id.prevBtn)
        nextBtn = findViewById(R.id.nextBtn)
        seekBar = findViewById(R.id.seekBar)
        timeView = findViewById(R.id.timeView)
        autoPlaySwitch = findViewById(R.id.switch1)


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
                    seekBarisHeld = true // start seeking in track
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekBarThreadRunning) {
                    MusicService.musicPlayer.setTrackProgress(seekBar.progress)
                    seekBarisHeld = false // end seeking in track
                }
            }
        })

        // receiver that handles input from media playback notification
        // also updates the MainActivity UI when an action happens in notification

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

        // unregister old receiver from previous context when main activity re-opens
        // and register new receiver in new context

        if (MusicService.mediaReceiverInit) {
            MusicService.oldContext.unregisterReceiver(MusicService.mediaReceiver)
            MusicService.mediaReceiver = mediaControlsReceiver
            MusicService.oldContext = this
            registerReceiver(MusicService.mediaReceiver, IntentFilter("MusicServiceIntent"))
        } else {
            MusicService.oldContext = this
            MusicService.mediaReceiverInit = true
            MusicService.mediaReceiver = mediaControlsReceiver
            registerReceiver(MusicService.mediaReceiver, IntentFilter("MusicServiceIntent"))
        }

        tracksLoadedView = findViewById(R.id.textView2)
        tracksLoadedView.text = audioFilesUri.size.toString() + " tracks loaded"


        if (MusicService.audioFilesUriCopy != null) {
            audioFilesUri = ArrayList(MusicService.audioFilesUriCopy!!)
            refreshTrackList()
        } else {
            val settings = getSharedPreferences(PREFS_NAME, 0)

            val retrievedFolders = settings.getStringSet("stringFolders", null)

            // Scan previously accessed folders (if they exist)
            if (retrievedFolders != null) {
                for (strFolder in retrievedFolders) {
                    scanFolderUri(strFolder.toUri())
                }
                refreshTrackList()
            }
        }


        // set on click listeners for UI buttons

        selectFolderBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(Intent.createChooser(intent, "Choose directory"), folderPickRequestCode);
        }

        playbackBtn.setOnClickListener {
            try {
                if (currentTrackIndex != -1) {
                    if (!isMusicServiceRunning())
                        MusicService.startService(this)
                    toggleMusicPlayback()
                }
            } catch (e : Exception) {
                Toast.makeText(this, "Error toggling playback : $e", Toast.LENGTH_LONG).show()
            }

        }

        stopBtn.setOnClickListener {
            try {
                stopPlayback()
            } catch (e : Exception) {
                Toast.makeText(this, "Error stopping playback : $e", Toast.LENGTH_LONG).show()
            }
        }

        prevBtn.setOnClickListener {
            try {
                prevTrack()
            } catch (e : Exception) {
                Toast.makeText(this, "Error moving to previous track : $e", Toast.LENGTH_LONG).show()
            }
        }

        nextBtn.setOnClickListener {
            try {
                nextTrack()
            } catch (e : Exception) {
                Toast.makeText(this, "Error moving to next track : $e", Toast.LENGTH_LONG).show()
            }

        }

        MusicService.musicPlayer.autoPlay = autoPlaySwitch.isChecked

        autoPlaySwitch.setOnClickListener {
            MusicService.musicPlayer.autoPlay = autoPlaySwitch.isChecked
        }

        if (isMusicServiceRunning()) {
            loadDataFromService()
        }

        val spinner = findViewById<Spinner>(R.id.spinner)

        spinner.onItemSelectedListener =
            object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    if (actFirstLaunch) {
                        actFirstLaunch = false
                        return
                    }
                    stopPlayback()
                    when (pos) {
                        0 -> sortByFileName(true)
                        1 -> sortByFileName(false)
                        2 -> sortByDate(true)
                        3 -> sortByDate(false)
                        4 -> shuffleTrackList()
                    }
                    refreshTrackList()
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {
                    // TODO Auto-generated method stub
                }
            }
    }

    // function to load data from running service, if new instance of MainActivity is opened
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

        val seekBarThread = createSeekBarThread()

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
        recyclerView.adapter?.notifyDataSetChanged()
        val (title, artist) = MusicService.musicPlayer.getMetaData(this, audioFilesUri[trackIndex])
        if (title == null || artist == null)
            currentTrackView.text = trackList[trackIndex].getFileName() + "\n" + "No metadata"
        else
            currentTrackView.text = "Title : $title\nArtist : $artist"


        seekBarisHeld = false
        seekBar.isEnabled = true

        val seekBarThread = createSeekBarThread()

        if (!seekBarThreadRunning) {
            seekBarThreadRunning = true
            seekBarThread.start()
        }

        // if autoplay is checked, play track right after selecting it
        if (autoPlaySwitch.isChecked){
            MusicService.musicPlayer.play()
            playbackBtn.text = "Pause"
            (playbackBtn as MaterialButton).icon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause)
        }
    }

    @SuppressLint("SetTextI18n")
    fun stopPlayback() {
        if (!MusicService.musicPlayer.isStopped()) {
            seekBarThreadRunning = false
            seekBar.isEnabled = false
            if (isMusicServiceRunning()) {
                MusicService.musicPlayer.stop()
                MusicService.stopService(this)
            }
            currentTrackView.text = "No track selected"
            currentTrackIndex = -1
            MusicService.trackIndex = -1
            recyclerView.adapter?.notifyDataSetChanged()
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
        // rollback index to beginning on end, so it is always in valid range
        if (currentTrackIndex != -1) {
            currentTrackIndex = if (currentTrackIndex == audioFilesUri.size - 1)
                0
            else
                currentTrackIndex!! + 1
            selectTrack(currentTrackIndex!!)
        }
    }

    fun prevTrack() {
        // rollback index to beginning on end, so it is always in valid range
        if (currentTrackIndex != -1) {
            currentTrackIndex = if (currentTrackIndex == 0)
                audioFilesUri.size - 1
            else
                currentTrackIndex!! - 1
            selectTrack(currentTrackIndex!!)
        }
    }

    private fun sortByFileName(ascending: Boolean) {
        if (audioFilesUri.size > 1) {
            val sorted = audioFilesUri.toTypedArray()
            Arrays.sort(sorted) { object1, object2 ->
                val audioFileDoc1: DocumentFile? = DocumentFile.fromSingleUri(baseContext, object1)
                val audioFileDoc2: DocumentFile? = DocumentFile.fromSingleUri(baseContext, object2)

                val filename1 = audioFileDoc1?.name
                val filename2 = audioFileDoc2?.name

                if (filename1 != null && filename2 != null) {
                    if (ascending)
                        filename1.compareTo(filename2)
                    else
                        filename2.compareTo(filename1)
                } else {
                    0
                }
            }

            audioFilesUri = sorted.toCollection(ArrayList())
        }
    }

    private fun sortByDate(ascending: Boolean) {
        if (audioFilesUri.size > 1) {
            val sorted = audioFilesUri.toTypedArray()
            Arrays.sort(sorted) { object1, object2 ->
                val audioFileDoc1: DocumentFile? = DocumentFile.fromSingleUri(baseContext, object1)
                val audioFileDoc2: DocumentFile? = DocumentFile.fromSingleUri(baseContext, object2)

                val f1 = Date(audioFileDoc1!!.lastModified())
                val f2 = Date(audioFileDoc2!!.lastModified())

                if (ascending) {
                    if (f1.before(f2)) {
                        f1.time.toInt()
                    } else {
                        f2.time.toInt()
                    }
                } else {
                    if (f1.after(f2)) {
                        f1.time.toInt()
                    } else {
                        f2.time.toInt()
                    }
                }

            }

            audioFilesUri = sorted.toCollection(ArrayList())
        }
    }

    private fun shuffleTrackList() {
        audioFilesUri = audioFilesUri.shuffled() as ArrayList<Uri>
    }

    @SuppressLint("SetTextI18n")
    private fun refreshTrackList() {
        trackList = ArrayList()
        for ((index, audioFileUri) in audioFilesUri.withIndex()) {
            try {
                val audioFileDoc : DocumentFile? = DocumentFile.fromSingleUri(this, audioFileUri)
                val filename = audioFileDoc?.name
                val (title, artist) = MusicService.musicPlayer.getMetaData(this, audioFileUri)
                trackList.add(ListItemData(filename, title, artist, index))
            } catch (e : Exception) {
                Toast.makeText(this, "Unable to load file, Error : $e", Toast.LENGTH_LONG).show()
            }
        }
        recyclerView = findViewById(R.id.recyclerView)
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
                        // avoid duplicate file paths
                        if (fileExtension in musicFileExtensions && file.uri !in audioFilesUri)
                            audioFilesUri.add(file.uri)
                    }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == folderPickRequestCode) {
            val contentResolver = applicationContext.contentResolver

            // request persistent scoped file access for the folder selected by the user
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            data?.data?.let { contentResolver.takePersistableUriPermission(it, takeFlags) }

            data?.data?.let { foldersAddedUri.add(it) }

            // scan folder for music files
            scanFolderUri(data?.data)

            // save the paths of music folders in app preferences storage
            val settings = getSharedPreferences(PREFS_NAME, 0)
            val editor = settings.edit()

            val retrievedFolders = settings.getStringSet("stringFolders", null)

            if (retrievedFolders != null) {
                editor.remove("stringFolders")
            }

            val stringFolderSet : MutableSet<String> = mutableSetOf()

            for (folder in foldersAddedUri) {
                val uriStr = folder.toString()
                // avoid duplicate folder paths
                if (uriStr !in stringFolderSet)
                    stringFolderSet.add(uriStr)
            }

            editor.putStringSet("stringFolders", stringFolderSet)

            editor.commit()

            refreshTrackList()
        }
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onDestroy() {
        // stop seekbar thread when activity is closed by user
        seekBarThreadRunning = false
        MusicService.audioFilesUriCopy = ArrayList(audioFilesUri)
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