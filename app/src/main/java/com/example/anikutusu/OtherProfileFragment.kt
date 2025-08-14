package com.example.anikutusu

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.anikutusu.databinding.FragmentOtherProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class OtherProfileFragment : Fragment() {


    private lateinit var binding:FragmentOtherProfileBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        binding= FragmentOtherProfileBinding.inflate(inflater,container,false)

        var userName=arguments?.getString("username")
        var mail=arguments?.getString("email")
        var imgLogo=arguments?.getString("imgLogo")

        binding.userName.text=userName

        binding.followButton.setOnClickListener {

            var database=FirebaseDatabase.getInstance()
            var auth=FirebaseAuth.getInstance()
            var userList=database.getReference("Users")
            var user=binding.userName.text.toString()
            var loginUser=auth.currentUser?.displayName
            var userKey=user+"Data"
            var loginUserKey=loginUser+"Data"

            Log.e("deneme",loginUser.toString())

            userList.child(loginUserKey).child("userFollowingList").child(loginUser+"Followed"+user).setValue(user)
            userList.child(userKey).child("userFollowerList").child(user+"Follower"+loginUser).setValue(loginUser)


        }











        return binding.root
    }


}