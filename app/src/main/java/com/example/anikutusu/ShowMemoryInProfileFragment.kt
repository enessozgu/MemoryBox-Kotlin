package com.example.anikutusu

import android.animation.Animator
import android.os.Bundle
import android.os.SystemClock
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.anikutusu.databinding.FragmentShowMemoryInProfileBinding

class ShowMemoryInProfileFragment : Fragment() {

    private var _binding: FragmentShowMemoryInProfileBinding? = null
    private val binding get() = _binding!!

    private var isLiked = false
    private var lastDoubleTapTs = 0L   // debounce için

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowMemoryInProfileBinding.inflate(inflater, container, false)
        setupDoubleTap()
        setupLikeButton()
        binding.comment.setOnClickListener {
            CommentsBottomSheetFragment().show(parentFragmentManager, "comments")
        }

        return binding.root
    }

    private fun setupDoubleTap() {
        val detector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {

                // Single tap'i tamamen YOK SAY
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean = true

                // onDown true dön; yoksa bazı cihazlarda double tap yakalanmayabilir
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    // Debounce: aynı double-tap zincirinden ikinci toggle'ı engelle
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastDoubleTapTs < 250) return true
                    lastDoubleTapTs = now

                    // Çift tıkta her zaman toggle et (beğeniliyse beğeniyi kaldır)
                    toggleLike()
                    return true
                }
            }
        )

        binding.sharedImg.setOnTouchListener { v, event ->
            detector.onTouchEvent(event)
            // Erişilebilirlik uyarısı için bırakılabilir; tıklama action'ı yoksa toggle etmez
            if (event.action == MotionEvent.ACTION_UP) v.performClick()
            true
        }
    }

    private fun setupLikeButton() {
        binding.like.setOnClickListener { toggleLike() }
    }

    private fun toggleLike() {
        isLiked = !isLiked
        if (isLiked) {
            binding.like.setBackgroundResource(R.drawable.fullhearth) // dolu kalp
            playHeartAnim()
            // TODO: backend'e like kaydı
        } else {
            binding.like.setBackgroundResource(R.drawable.heart)      // boş kalp
            // TODO: backend'den like sil
        }
    }

    private fun playHeartAnim() {
        binding.heartAnim.apply {
            visibility = View.VISIBLE
            progress = 0f
            playAnimation()
            addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) { visibility = View.GONE }
                override fun onAnimationCancel(animation: Animator) { visibility = View.GONE }
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
