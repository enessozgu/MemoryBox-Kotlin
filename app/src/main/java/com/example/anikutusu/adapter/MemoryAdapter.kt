package com.example.anikutusu.adapter

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anikutusu.R
import com.example.anikutusu.model.MemoryItem

// Adapter class for displaying a list of MemoryItem in a RecyclerView
class MemoryAdapter(
    private val onItemClick: (MemoryItem) -> Unit // Lambda function triggered when an item is clicked
) : RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder>() {

    // List to hold memory items
    private val memoryList = mutableListOf<MemoryItem>()

    // MediaPlayer for audio playback
    private var mediaPlayer: MediaPlayer? = null

    // Keeps track of which audio item is currently playing
    private var currentlyPlayingPosition: Int? = null

    // ViewHolder class to hold views for each memory item
    inner class MemoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.textMemoryItem) // Text of the memory
        val image: ImageView = itemView.findViewById(R.id.imageMemoryItem) // Image of the memory
    }

    // Creates a new ViewHolder when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memory, parent, false)
        return MemoryViewHolder(view)
    }

    // Binds data to the views in the ViewHolder
    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val memory = memoryList[position]
        holder.text.text = memory.text // Set memory text

        // If there's an image URL, load it with Glide and make the ImageView visible
        if (memory.photoUrl != null) {
            holder.image.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(memory.photoUrl)
                .into(holder.image)
        } else {
            holder.image.visibility = View.GONE // Hide image if there is no photo URL
        }

        // Handle general item click
        holder.itemView.setOnClickListener {
            onItemClick(memory)
        }

        // Get references to the Play and Stop buttons
        val buttonPlay = holder.itemView.findViewById<Button>(R.id.buttonPlayAudio)
        val buttonStop = holder.itemView.findViewById<Button>(R.id.buttonStopAudio)

        // Check if there is an audio URL for the memory
        if (!memory.audioUrl.isNullOrEmpty()) {
            buttonPlay.visibility = View.VISIBLE
            buttonStop.visibility = View.VISIBLE

            // Play audio button click
            buttonPlay.setOnClickListener {
                try {
                    // Stop previous audio if playing
                    mediaPlayer?.release()

                    // Initialize MediaPlayer
                    mediaPlayer = MediaPlayer().apply {
                        setOnPreparedListener {
                            it.start() // Start playing audio
                            Toast.makeText(holder.itemView.context, "Playing audio", Toast.LENGTH_SHORT).show()
                        }
                        setOnCompletionListener {
                            release()
                            mediaPlayer = null
                            currentlyPlayingPosition = null
                            notifyItemChanged(holder.adapterPosition) // Refresh item view
                        }
                        setDataSource(memory.audioUrl) // Set audio source
                        prepareAsync() // Prepare media asynchronously
                    }
                    currentlyPlayingPosition = holder.adapterPosition
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, "Could not play audio: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            // Stop audio button click
            buttonStop.setOnClickListener {
                if (currentlyPlayingPosition == holder.adapterPosition) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    currentlyPlayingPosition = null
                    Toast.makeText(holder.itemView.context, "Audio stopped", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Hide play/stop buttons if there is no audio
            buttonPlay.visibility = View.GONE
            buttonStop.visibility = View.GONE
        }
    }

    // Returns the total number of memory items
    override fun getItemCount(): Int = memoryList.size

    // Updates the memory list and refreshes the RecyclerView
    fun updateList(newList: List<MemoryItem>) {
        memoryList.clear()
        memoryList.addAll(newList)
        notifyDataSetChanged()
    }
}
