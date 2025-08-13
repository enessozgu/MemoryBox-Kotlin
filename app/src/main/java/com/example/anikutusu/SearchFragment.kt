package com.example.anikutusu

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.anikutusu.adapter.UserAdapter
import com.example.anikutusu.databinding.FragmentSearchBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var adapter: UserAdapter
    private lateinit var userList: ArrayList<UserDataClass>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        // RecyclerView & Adapter
        userList = ArrayList()
        binding.rv.setHasFixedSize(true)
        binding.rv.layoutManager =
            StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        adapter = UserAdapter(requireContext(), userList)
        binding.rv.adapter = adapter

        // Giriş yapan kullanıcının maili (küçük harfe normalize)
        val currentUserMail = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase()

        // Arama dinleyicisi
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s?.toString()?.trim().orEmpty()
                val db = FirebaseDatabase.getInstance()
                val usersRef = db.getReference("Users")

                val query: Query = if (searchText.isEmpty()) {
                    // Boşsa tüm kullanıcılar (alfabetik)
                    usersRef.orderByChild("userName")
                } else {
                    // Başlayanlara göre filtre + alfabetik
                    usersRef.orderByChild("userName")
                        .startAt(searchText)
                        .endAt(searchText + "\uf8ff")
                }

                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        userList.clear()

                        for (userSnap in snapshot.children) {
                            val userName = userSnap.child("userName").getValue(String::class.java)
                            val userMail = userSnap.child("userMail").getValue(String::class.java)

                            // Geçerli kayıt + giriş yapan kullanıcı değilse ekle
                            val mailNormalized = userMail?.trim()?.lowercase()
                            if (!userName.isNullOrEmpty()
                                && !mailNormalized.isNullOrEmpty()
                                && mailNormalized != currentUserMail
                            ) {
                                userList.add(UserDataClass(userName, userMail!!, R.drawable.images))
                            }
                        }

                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        return binding.root
    }
}
