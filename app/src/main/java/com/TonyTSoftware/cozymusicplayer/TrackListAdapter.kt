package com.TonyTSoftware.cozymusicplayer
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrackListAdapter(trackList : ArrayList<ListItemData>) : RecyclerView.Adapter<TrackListAdapter.ViewHolder>(){
    private var trackList : ArrayList<ListItemData> = trackList
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater : LayoutInflater = LayoutInflater.from(MainActivity.mainActivityCont)
        val listItem : View = layoutInflater.inflate(R.layout.iteminlist, parent, false)
        return ViewHolder(listItem)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item : ListItemData = trackList[position]
        val filename = item.getFileName()
        val title = item.getTrackTitle()
        val artist = item.getTrackArtist()
        if (title == null || artist == null)
            holder.trackTextView.text = filename
        else
            holder.trackTextView.text = title + " - " + artist
        holder.trackIndex = item.getTrackIndex()
        holder.relativeLayout.setOnClickListener {
            MainActivity.mainActivityPtr.selectTrack(holder.trackIndex!!)
        }
    }

    override fun getItemCount(): Int {
        return trackList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var trackTextView : TextView = itemView.findViewById(R.id.textView)
        var trackIndex : Int? = null
        var relativeLayout : RelativeLayout = itemView.findViewById(R.id.relativeLayout)
    }
}