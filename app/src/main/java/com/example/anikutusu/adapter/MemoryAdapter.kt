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

class MemoryAdapter(
    private val onItemClick: (MemoryItem) -> Unit
) : RecyclerView.Adapter<MemoryAdapter.MemoryViewHolder>() {

    private val memoryList = mutableListOf<MemoryItem>()
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingPosition: Int? = null



    inner class MemoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.textMemoryItem)
        val image: ImageView = itemView.findViewById(R.id.imageMemoryItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memory, parent, false)

        return MemoryViewHolder(view)
    }



    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val memory = memoryList[position]
        holder.text.text = memory.text

        if (memory.photoUrl != null) {
            holder.image.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(memory.photoUrl)
                .into(holder.image)
        } else {
            holder.image.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(memory)
        }

        val buttonPlay = holder.itemView.findViewById<Button>(R.id.buttonPlayAudio)
        val buttonStop = holder.itemView.findViewById<Button>(R.id.buttonStopAudio)

        if (!memory.audioUrl.isNullOrEmpty()) {
            buttonPlay.visibility = View.VISIBLE
            buttonStop.visibility = View.VISIBLE

            buttonPlay.setOnClickListener {
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setOnPreparedListener {
                            it.start()
                            Toast.makeText(holder.itemView.context, "Ses oynatılıyor", Toast.LENGTH_SHORT).show()
                        }
                        setOnCompletionListener {
                            release()
                            mediaPlayer = null
                            currentlyPlayingPosition = null
                            notifyItemChanged(holder.adapterPosition)
                        }
                        setDataSource(memory.audioUrl)
                        prepareAsync()  // burada senkron prepare() yerine async kullan
                    }
                    currentlyPlayingPosition = holder.adapterPosition
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, "Ses oynatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }


            buttonStop.setOnClickListener {
                if (currentlyPlayingPosition == holder.adapterPosition) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    currentlyPlayingPosition = null
                    Toast.makeText(holder.itemView.context, "Ses durduruldu", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            buttonPlay.visibility = View.GONE
            buttonStop.visibility = View.GONE
        }






    }



    override fun getItemCount(): Int = memoryList.size

    fun updateList(newList: List<MemoryItem>) {
        memoryList.clear()
        memoryList.addAll(newList)
        notifyDataSetChanged()
    }
}