package com.example.anikutusu.adapter

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.google.firebase.firestore.FirebaseFirestore
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
        val buttonPlay: Button = itemView.findViewById(R.id.buttonPlayAudio)
        val buttonStop: Button = itemView.findViewById(R.id.buttonStopAudio)
        val buttonDelete: Button = itemView.findViewById(R.id.memorydelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memory, parent, false)
        return MemoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val memory = memoryList[position]

        holder.text.text = memory.text

        // Fotoğraf zoom dialog
        holder.image.setOnClickListener {
            val adapterPos = holder.adapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

            memoryList.getOrNull(adapterPos)?.photoUrl?.let { url ->
                val dialogView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.dialog_fullscreen_image, null)
                val zoomImageView = dialogView.findViewById<ImageView>(R.id.zoomImageView)
                Glide.with(holder.itemView.context).load(url).into(zoomImageView)

                val dialog = AlertDialog.Builder(holder.itemView.context)
                    .setView(dialogView)
                    .create()

                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))


                dialog.show()
            }
        }

        // Fotoğraf varsa göster, yoksa gizle
        if (memory.photoUrl != null) {
            holder.image.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(memory.photoUrl)
                .into(holder.image)
        } else {
            holder.image.visibility = View.GONE
        }

        // Item genel tıklama
        holder.itemView.setOnClickListener {
            val adapterPos = holder.adapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

            memoryList.getOrNull(adapterPos)?.let { item ->
                onItemClick(item)
            }
        }

        // Ses dosyası varsa butonları göster
        if (!memory.audioUrl.isNullOrEmpty()) {
            holder.buttonPlay.visibility = View.VISIBLE
            holder.buttonStop.visibility = View.VISIBLE

            val isPlaying = (currentlyPlayingPosition == holder.adapterPosition)
            holder.buttonPlay.isEnabled = !isPlaying
            holder.buttonStop.isEnabled = isPlaying

            holder.buttonPlay.setOnClickListener {
                val adapterPos = holder.adapterPosition
                if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

                val audioUrl = memoryList.getOrNull(adapterPos)?.audioUrl
                if (audioUrl.isNullOrEmpty()) return@setOnClickListener

                try {
                    mediaPlayer?.release()

                    mediaPlayer = MediaPlayer().apply {
                        setOnPreparedListener {
                            it.start()
                            Toast.makeText(holder.itemView.context, "Playing audio", Toast.LENGTH_SHORT).show()
                            notifyItemChanged(adapterPos)
                        }
                        setOnCompletionListener {
                            release()
                            mediaPlayer = null
                            currentlyPlayingPosition = null
                            notifyItemChanged(adapterPos)
                        }
                        setDataSource(audioUrl)
                        prepareAsync()
                    }
                    currentlyPlayingPosition = adapterPos
                    notifyItemChanged(adapterPos)
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, "Could not play audio: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            holder.buttonStop.setOnClickListener {
                val adapterPos = holder.adapterPosition
                if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (currentlyPlayingPosition == adapterPos) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    currentlyPlayingPosition = null
                    Toast.makeText(holder.itemView.context, "Audio stopped", Toast.LENGTH_SHORT).show()
                    notifyItemChanged(adapterPos)
                }
            }
        } else {
            holder.buttonPlay.visibility = View.GONE
            holder.buttonStop.visibility = View.GONE
        }

        // Silme butonu
        holder.buttonDelete.setOnClickListener {
            val adapterPos = holder.adapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener

            memoryList.getOrNull(adapterPos)?.id?.let { id ->
                deleteMemory(id, adapterPos)
            }
        }
    }

    override fun getItemCount(): Int = memoryList.size

    fun updateList(newList: List<MemoryItem>) {
        memoryList.clear()
        memoryList.addAll(newList)
        notifyDataSetChanged()
    }

    fun deleteMemory(memoryId: String, position: Int) {
        val db = FirebaseFirestore.getInstance()
        db.collection("memories")
            .document(memoryId)
            .delete()
            .addOnSuccessListener {
                memoryList.removeAt(position)
                notifyItemRemoved(position)
                println("Anı başarıyla silindi.")
            }
            .addOnFailureListener { e ->
                println("Anı silinirken hata oluştu: ${e.message}")
            }
    }

    override fun onViewRecycled(holder: MemoryViewHolder) {
        super.onViewRecycled(holder)
        if (currentlyPlayingPosition == holder.adapterPosition) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentlyPlayingPosition = null
        }
    }
}
