package com.TonyTSoftware.cozymusicplayer

import android.net.Uri

class ListItemData(
    private var fileName: String?,
    private var trackTitle: String?,
    private var trackArtist: String?,
    private var trackIndex: Int
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

    fun getTrackIndex() : Int {
        return trackIndex
    }
}