package com.example.anikutusu.adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.example.anikutusu.R
import com.example.anikutusu.UserDataClass

class FollowInfoAdapter(private val mContext:Context,private val userList:List<UserDataClass>) : RecyclerView.Adapter<FollowInfoAdapter.MyViewHolder>(){
    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cl: ConstraintLayout = view.findViewById(R.id.cl2)
        val imgLogo: ImageView = view.findViewById(R.id.imgLogo)
        val username: TextView = view.findViewById(R.id.username)
    }

    override fun getItemCount(): Int = userList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val design = LayoutInflater.from(mContext).inflate(R.layout.cardview, parent, false)
        return MyViewHolder(design)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = userList[position]

        holder.username.text = item.username


        holder.cl.setOnClickListener {v->
            val bundle= Bundle().apply {
                putString("username",item.username)
                putString("imgLogo",item.photoImage.toString())
                putString("email",item.email)
            }

            Navigation.findNavController(v).navigate(R.id.action_global_otherProfileFragment,bundle)

        }

    }
}