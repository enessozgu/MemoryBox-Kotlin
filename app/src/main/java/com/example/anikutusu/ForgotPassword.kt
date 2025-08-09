package com.example.anikutusu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.anikutusu.databinding.FragmentForgotPasswordBinding

class ForgotPassword : Fragment() {


    private lateinit var binding:FragmentForgotPasswordBinding
    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding=FragmentForgotPasswordBinding.inflate(inflater,container,false)




        return binding.root
    }



}