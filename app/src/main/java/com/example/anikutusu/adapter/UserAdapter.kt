package com.example.anikutusu.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.anikutusu.R
import com.example.anikutusu.UserDataClass

class UserAdapter(private val mContext: Context, private var userList: List<UserDataClass>) : RecyclerView.Adapter<UserAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cl: ConstraintLayout = view.findViewById(R.id.cl)
        val imgLogo: ImageView = view.findViewById(R.id.imgLogo)
        val username: TextView = view.findViewById(R.id.username)
    }

    override fun getItemCount(): Int = userList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val design = LayoutInflater.from(mContext).inflate(R.layout.add_user_cardview, parent, false)
        return MyViewHolder(design)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = userList[position]

        holder.username.text = item.username


        holder.cl.setOnClickListener {

        }
    }



}
