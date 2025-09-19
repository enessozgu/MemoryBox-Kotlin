package com.example.anikutusu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.anikutusu.adapter.CommentAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.UUID
class CommentsBottomSheetFragment : BottomSheetDialogFragment() {

    // Bu fragment için kullanılacak dialog teması
    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme


    private lateinit var rv: RecyclerView
    private lateinit var et: EditText
    private lateinit var btnSend: ImageButton
    private val adapter = CommentAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_comments, container, false)
        rv = view.findViewById(R.id.rvComments)
        et = view.findViewById(R.id.etComment)
        btnSend = view.findViewById(R.id.btnSend)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        adapter.submit(
            listOf(
                Comment("1","ozgurozel","Halk iradesine el sürmenin bir sınırı vardır..."),
                Comment("2","ekrem imamoglu","Pasif direniş sonuç getirmez, net."),
                Comment("3","uzi","Ebo beni dövdü"),
                Comment("4","lvbelc5","Baba porşa biner"),
                Comment("5","umutyigit","Babayla zor yarışırlarrr")
            )
        )

        btnSend.setOnClickListener {
            val text = et.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            adapter.add(Comment(UUID.randomUUID().toString(), "yigit", text))
            et.setText("")
            rv.scrollToPosition(0)
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }
}
