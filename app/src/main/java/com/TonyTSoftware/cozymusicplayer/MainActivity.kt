package com.TonyTSoftware.cozymusicplayer

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {
    private val folderPickRequestCode = 100
    private var audioFilesUri : ArrayList<Uri> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val selectFolderBtn : Button = findViewById(R.id.selectfolderbtn)
        selectFolderBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(Intent.createChooser(intent, "Choose directory"), folderPickRequestCode);
        }
    }

    private fun refreshUI() {
        val trackList : ArrayList<ListItemData> = ArrayList()
        for (audioFileUri in audioFilesUri) {
            val audioFileDoc : DocumentFile? = DocumentFile.fromSingleUri(this, audioFileUri)
            val filename = audioFileDoc?.name
            val metaDataRetriever = MediaMetadataRetriever()
            metaDataRetriever.setDataSource(this, audioFileUri)
            val artist : String? = metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val title : String? = metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            trackList.add(ListItemData(filename, title, artist, audioFileUri))
        }
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
                            val fileExtension = file.name?.substringAfterLast('.', "")
                            if (fileExtension == "mp3" || fileExtension == "ogg" || fileExtension == "flac")
                                audioFilesUri.add(file.uri)
                        }
                refreshUI()
            }
        }
    }
}