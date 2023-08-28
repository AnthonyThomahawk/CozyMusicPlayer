package com.TonyTSoftware.cozymusicplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile


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

    private fun refreshTextView() {
        val textView : TextView = findViewById(R.id.textView)
        textView.text = "Music files currently loaded : \n"
        var i = 1
        for (audioFile in audioFilesUri) {
            val audioFiledoc : DocumentFile? = DocumentFile.fromSingleUri(this, audioFile)
            val filename = audioFiledoc?.name
            textView.text = textView.text as String + i.toString() + ")" + filename + "\n"
            i++

            // below code returns null if no metadata exists, will keep this for later
//            val metaDataRetriever = MediaMetadataRetriever()
//            metaDataRetriever.setDataSource(this, audioFile)
//            val artist : String? = metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
//            val title : String? = metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == folderPickRequestCode) {
            val selectedFolderUri = data?.data
            if (selectedFolderUri != null) {
                val selectedFolder : DocumentFile? = DocumentFile.fromTreeUri(this, selectedFolderUri!!)
                val filesInFolder = selectedFolder?.listFiles()
                if (filesInFolder != null)
                    for (file in filesInFolder)
                        if (file.isFile) {
                            val fileExtension = file.name?.substringAfterLast('.', "")
                            if (fileExtension == "mp3" || fileExtension == "ogg" || fileExtension == "flac")
                                audioFilesUri.add(file.uri)
                        }
                refreshTextView()
            }
        }
    }
}