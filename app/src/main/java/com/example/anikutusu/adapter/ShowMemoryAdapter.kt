package com.example.anikutusu.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.anikutusu.R
import com.example.anikutusu.ShowMemoryDataClass

class ShowMemoryAdapter(private var mContext:Context,private var memoryList:ArrayList<ShowMemoryDataClass>):RecyclerView.Adapter<ShowMemoryAdapter.holderConstraintLayout>() {

    inner class holderConstraintLayout(view:View):RecyclerView.ViewHolder(view){
        var sharedImg:ImageView
        init {
            sharedImg=view.findViewById(R.id.sharedImg)
        }
    }

    override fun getItemCount(): Int {

        return memoryList.size

    }

    override fun onBindViewHolder(holder: holderConstraintLayout, position: Int) {
        var myHolder=memoryList[position]
        holder.sharedImg.setImageResource(myHolder.sharedMemory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): holderConstraintLayout {

        var design=LayoutInflater.from(mContext).inflate(R.layout.showmemoryinprofiledesign,parent,false)
        return holderConstraintLayout(design)

    }


}