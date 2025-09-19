package com.example.anikutusu.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.anikutusu.Comment
import com.example.anikutusu.R

class CommentAdapter(
    private val items: MutableList<Comment> = mutableListOf()
) : RecyclerView.Adapter<CommentAdapter.VH>() {

    inner class VH(val v: View) : RecyclerView.ViewHolder(v) {
        val tvUser = v.findViewById<TextView>(R.id.tvUser)
        val tvText = v.findViewById<TextView>(R.id.tvText)
        val ivAvatar = v.findViewById<ImageView>(R.id.ivAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.tvUser.text = c.user
        holder.tvText.text = c.text
        // Glide ile avatar yükleme yapabilirsin
    }

    override fun getItemCount() = items.size

    fun submit(newList: List<Comment>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun add(comment: Comment) {
        items.add(0, comment) // en üste ekle
        notifyItemInserted(0)
    }
}
