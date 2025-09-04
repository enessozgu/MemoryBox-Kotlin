package com.example.anikutusu.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anikutusu.R
import com.example.anikutusu.ShowMemoryMainPageDataClass

class ShowMemoryInMainPageAdapter(
    private val mContext: Context,
    private val dataList: ArrayList<ShowMemoryMainPageDataClass>
) : RecyclerView.Adapter<ShowMemoryInMainPageAdapter.Holder>() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val sharedImg: ImageView = view.findViewById(R.id.sharedImg)
        val userName: TextView = view.findViewById(R.id.userName)
        val location: TextView = view.findViewById(R.id.location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val design = LayoutInflater.from(mContext)
            .inflate(R.layout.showotherusermemorydesign, parent, false)
        return Holder(design)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = dataList[position]
        holder.userName.text = item.userName
        holder.location.text = item.location

        Glide.with(holder.itemView)
            .load(item.imageUrl)
            .placeholder(R.drawable.secondimg)  // /res/drawable/placeholder.png koyabilirsin
            .error(R.drawable.secondimg)
            .into(holder.sharedImg)
    }
}
