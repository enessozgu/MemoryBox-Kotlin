package com.example.anikutusu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CollaboratorAdapter(
    private val items: List<SimpleUser>,
    private val onClick: (SimpleUser) -> Unit
) : RecyclerView.Adapter<CollaboratorAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvMail: TextView = itemView.findViewById(R.id.tvMail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collaborator, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = items[position]
        holder.tvName.text = u.userName
        holder.tvMail.text = u.userMail
        holder.itemView.setOnClickListener { onClick(u) }
    }

    override fun getItemCount(): Int = items.size
}
