package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Tab
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.anikutusu.databinding.FragmentHomePageBinding
import com.google.android.material.tabs.TabLayoutMediator

class HomePageFragment : Fragment() {

    private lateinit var fragmentListesi:ArrayList<Fragment>
    private lateinit var fragmentBaslikListesi:ArrayList<String>
    private lateinit var binding: FragmentHomePageBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomePageBinding.inflate(inflater, container, false)

        fragmentListesi= ArrayList()
        fragmentBaslikListesi= ArrayList()

        val mapFrag = HomeMapFragment().apply {
            arguments = bundleOf(
                "latitude" to -1f,
                "longitude" to -1f
            )
        }

        fragmentListesi.add(MainFragment())
        fragmentListesi.add(SearchFragment())
        fragmentListesi.add(ProfileFragment())
        fragmentListesi.add(TimeCapsule())


        fragmentBaslikListesi.add("Anasayfa")
        fragmentBaslikListesi.add("Ekle")
        fragmentBaslikListesi.add("Profil")
        fragmentBaslikListesi.add("Anı Haritası")

        var adapter=MyViewPagerAdapter(requireActivity())
        binding.vp.adapter=adapter


        /*
        TabLayoutMediator(binding.tl,binding.vp) { tab, position ->
            tab.text = fragmentBaslikListesi[position]
        }.attach()

         */

        TabLayoutMediator(binding.tl,binding.vp){tab,position->
            when(position){
                0->tab.setIcon(R.drawable.home)
                1->tab.setIcon(R.drawable.search)
                2->tab.setIcon(R.drawable.user)
                3->tab.setIcon(R.drawable.addpp)
            }
        }.attach()



        return binding.root
    }

    inner class MyViewPagerAdapter(fragmentActivity:FragmentActivity):FragmentStateAdapter(fragmentActivity){
        override fun getItemCount(): Int {
            return fragmentListesi.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragmentListesi[position]
        }
    }


}
