package com.TonyTSoftware.cozymusicplayer
import android.graphics.Paint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrackListAdapter(trackList : ArrayList<ListItemData>) : RecyclerView.Adapter<TrackListAdapter.ViewHolder>(){
    private var trackList : ArrayList<ListItemData> = trackList
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater : LayoutInflater = LayoutInflater.from(parent.context)
        val listItem : View = layoutInflater.inflate(R.layout.iteminlist, parent, false)
        return ViewHolder(listItem)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (MainActivity.mainActivityPtr.currentTrackIndex == -1) { // stopped
            holder.trackTextView.setTypeface(null, Typeface.NORMAL)
            holder.trackTextView.paintFlags = holder.trackTextView.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        }
        else if (position == MainActivity.mainActivityPtr.currentTrackIndex) { // highlight current track
            holder.trackTextView.setTypeface(null, Typeface.BOLD_ITALIC)
            holder.trackTextView.paintFlags = holder.trackTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        } else { // un-highlight all other tracks
            holder.trackTextView.setTypeface(null, Typeface.NORMAL)
            holder.trackTextView.paintFlags = holder.trackTextView.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        }

        val item : ListItemData = trackList[position]
        val filename = item.getFileName()
        val title = item.getTrackTitle()
        val artist = item.getTrackArtist()
        holder.trackTextView.text = filename
//        if (title == null || artist == null) // temporarily removed
//            holder.trackTextView.text = filename
//        else
//            holder.trackTextView.text = "$artist$ - $title"
        holder.trackIndex = item.getTrackIndex()

        holder.relativeLayout.setOnClickListener {
            MainActivity.mainActivityPtr.selectTrack(holder.trackIndex!!)
            holder.trackTextView.setTypeface(null, Typeface.BOLD_ITALIC)
            holder.trackTextView.paintFlags = holder.trackTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            notifyDataSetChanged()
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