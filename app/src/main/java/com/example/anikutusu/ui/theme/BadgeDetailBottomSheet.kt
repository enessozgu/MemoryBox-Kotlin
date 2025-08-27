package com.example.anikutusu.ui.badges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.anikutusu.R
import com.example.anikutusu.databinding.FragmentBadgeDetailBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BadgeDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentBadgeDetailBottomSheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_NAME = "arg_name"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_ICON = "arg_icon"
        private const val ARG_UNLOCKED = "arg_unlocked"
        private const val ARG_PROGRESS = "arg_progress"

        fun newInstance(
            id: String,
            name: String,
            desc: String,
            iconUrl: String?,
            unlocked: Boolean,
            progress: Int?
        ): BadgeDetailBottomSheet {
            val args = Bundle().apply {
                putString(ARG_ID, id)
                putString(ARG_NAME, name)
                putString(ARG_DESC, desc)
                putString(ARG_ICON, iconUrl)
                putBoolean(ARG_UNLOCKED, unlocked)
                progress?.let { putInt(ARG_PROGRESS, it) }
            }
            return BadgeDetailBottomSheet().apply { arguments = args }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBadgeDetailBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = arguments?.getString(ARG_NAME).orEmpty()
        val desc = arguments?.getString(ARG_DESC).orEmpty()
        val icon = arguments?.getString(ARG_ICON)
        val unlocked = arguments?.getBoolean(ARG_UNLOCKED) ?: false
        val progress = arguments?.getInt(ARG_PROGRESS, -1)?.takeIf { it >= 0 }



        // Başlık ve açıklama
        binding.badgeTitle.text = name
        binding.badgeDescription.text = desc

        // İkon yükleme
        if (!icon.isNullOrBlank()) {
            Glide.with(binding.badgeIcon.context)
                .load(icon)
                .placeholder(R.drawable.photo)
                .into(binding.badgeIcon)
        } else {
            binding.badgeIcon.setImageResource(R.drawable.loki)
        }

        // Kilit durumu
        binding.lockStateText.text = if (unlocked) "Açıldı" else "Kilidi Açık Değil"
        binding.lockStateImage.isVisible = !unlocked

        // İlerleme (progress bar)
        if (progress != null) {
            binding.progressContainer.isVisible = true
            binding.badgeProgress.progress = progress
            binding.progressText.text = "$progress%"
        } else {
            binding.progressContainer.isVisible = false
        }

        // Kapat butonu
        binding.closeBtn.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
