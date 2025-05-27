package com.holisticapp.fitness.ui.music

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.holisticapp.fitness.R

class PlaylistAdapter(
    private val playlist: List<MusicPlayerActivity.Song>,
    private val onSongClick: (MusicPlayerActivity.Song, Int) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.SongViewHolder>() {
    
    private var currentPlayingIndex = 0
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = playlist[position]
        holder.bind(song, position == currentPlayingIndex)
        
        holder.itemView.setOnClickListener {
            val oldIndex = currentPlayingIndex
            currentPlayingIndex = position
            notifyItemChanged(oldIndex)
            notifyItemChanged(currentPlayingIndex)
            onSongClick(song, position)
        }
    }
    
    override fun getItemCount(): Int = playlist.size
    
    fun setCurrentPlaying(index: Int) {
        val oldIndex = currentPlayingIndex
        currentPlayingIndex = index
        notifyItemChanged(oldIndex)
        notifyItemChanged(currentPlayingIndex)
    }
    
    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val songEmoji: TextView = itemView.findViewById(R.id.songEmoji)
        private val songTitle: TextView = itemView.findViewById(R.id.songTitle)
        private val songArtist: TextView = itemView.findViewById(R.id.songArtist)
        private val songDuration: TextView = itemView.findViewById(R.id.songDuration)
        private val spotifyButton: ImageButton? = itemView.findViewById(R.id.spotifyButton)
        
        fun bind(song: MusicPlayerActivity.Song, isCurrentlyPlaying: Boolean) {
            songEmoji.text = song.emoji
            songTitle.text = song.title
            songArtist.text = song.artist
            songDuration.text = formatDuration(song.duration)
            
            // Show/hide Spotify button based on whether we have a Spotify URL
            spotifyButton?.let { button ->
                if (song.spotifyUrl.isNotEmpty()) {
                    button.visibility = View.VISIBLE
                    button.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(song.spotifyUrl))
                        itemView.context.startActivity(intent)
                    }
                } else {
                    button.visibility = View.GONE
                }
            }
            
            // Highlight currently playing song
            if (isCurrentlyPlaying) {
                itemView.setBackgroundResource(R.drawable.song_item_selected)
                songTitle.setTextColor(itemView.context.getColor(R.color.primary_700))
            } else {
                itemView.setBackgroundResource(R.drawable.song_item_normal)
                songTitle.setTextColor(itemView.context.getColor(android.R.color.black))
            }
        }
        
        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d:%02d", minutes, remainingSeconds)
        }
    }
} 