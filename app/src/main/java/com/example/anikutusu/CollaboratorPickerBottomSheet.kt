package com.example.anikutusu

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class SimpleUser( // Zaten projende varsa bunu tekrar ekleme
    val userName: String = "",
    val userMail: String = ""
)

class CollaboratorPickerBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rv: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var adapter: CollaboratorAdapter
    private val users = ArrayList<SimpleUser>()

    private val usersRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("Users")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.dialog_pick_collaborator, container, false)
        rv = v.findViewById(R.id.rvCollaborators)
        etSearch = v.findViewById(R.id.etSearchCollaborator)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = CollaboratorAdapter(users) { selected ->
            setFragmentResult(
                REQUEST_KEY_COLLABORATOR,
                bundleOf(
                    BUNDLE_USER_NAME to selected.userName,
                    BUNDLE_USER_MAIL to selected.userMail
                )
            )
            dismiss()
        }
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                queryUsers(s?.toString()?.trim().orEmpty())
            }
        })

        queryUsers("") // ilk açılışta hepsi
        return v
    }

    private fun queryUsers(prefix: String) {
        val currentUserMail = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase()

        val q: Query = if (prefix.isEmpty()) {
            usersRef.orderByChild("userName")
        } else {
            usersRef.orderByChild("userName").startAt(prefix).endAt(prefix + "\uf8ff")
        }

        q.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (u in snapshot.children) {
                    val userName = u.child("userName").getValue(String::class.java)
                    val userMail = u.child("userMail").getValue(String::class.java)
                    val mailNorm = userMail?.trim()?.lowercase()
                    if (!userName.isNullOrEmpty() && !mailNorm.isNullOrEmpty() && mailNorm != currentUserMail) {
                        users.add(SimpleUser(userName, userMail!!))
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    companion object {
        const val REQUEST_KEY_COLLABORATOR = "request_key_collaborator"
        const val BUNDLE_USER_NAME = "bundle_user_name"
        const val BUNDLE_USER_MAIL = "bundle_user_mail"
    }
}
