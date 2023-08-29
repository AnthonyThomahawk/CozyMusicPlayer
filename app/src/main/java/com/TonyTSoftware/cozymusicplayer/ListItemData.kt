package com.TonyTSoftware.cozymusicplayer

import android.net.Uri

class ListItemData(
    private var fileName: String?,
    private var trackTitle: String?,
    private var trackArtist: String?,
    private var trackURI: Uri
) {

    fun getFileName() : String? {
        return fileName
    }

    fun getTrackTitle() : String? {
        return trackTitle
    }

    fun getTrackArtist() : String? {
        return trackArtist
    }

    fun getTrackUri() : Uri {
        return trackURI
    }
}