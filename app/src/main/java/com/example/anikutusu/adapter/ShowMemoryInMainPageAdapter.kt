package com.example.anikutusu.adapter

import android.content.Context
import android.os.SystemClock
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.anikutusu.R
import com.example.anikutusu.ShowMemoryMainPageDataClass

class ShowMemoryInMainPageAdapter(
    private val mContext: Context,
    private val dataList: ArrayList<ShowMemoryMainPageDataClass>,
    private val onLikeChanged: (item: ShowMemoryMainPageDataClass, position: Int, liked: Boolean) -> Unit,
    private val onCommentClick: (item: ShowMemoryMainPageDataClass, position: Int) -> Unit
) : RecyclerView.Adapter<ShowMemoryInMainPageAdapter.Holder>() {

    companion object {
        private const val PAYLOAD_LIKE_CHANGED = "payload_like_changed"
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val sharedImg: ImageView = view.findViewById(R.id.sharedImg)
        val userName: TextView = view.findViewById(R.id.userName)
        val location: TextView = view.findViewById(R.id.location)
        val likeBtn: Button = view.findViewById(R.id.like)
        val commentBtn: Button = view.findViewById(R.id.comment)
        val heartAnim: LottieAnimationView = view.findViewById(R.id.heartAnim)

        var lastDoubleTapTs: Long = 0L

        fun bindDoubleTap(detector: GestureDetector) {
            sharedImg.setOnTouchListener { v, event ->
                detector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) v.performClick()
                true
            }
        }

        fun playHeartAnim() {
            heartAnim.apply {
                visibility = View.VISIBLE
                progress = 0f
                playAnimation()
                addAnimatorListener(object : android.animation.Animator.AnimatorListener {
                    override fun onAnimationStart(animation: android.animation.Animator) {}
                    override fun onAnimationEnd(animation: android.animation.Animator) { visibility = View.GONE }
                    override fun onAnimationCancel(animation: android.animation.Animator) { visibility = View.GONE }
                    override fun onAnimationRepeat(animation: android.animation.Animator) {}
                })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val design = LayoutInflater.from(mContext)
            .inflate(R.layout.showmemoryinprofilerealdesign, parent, false)
        return Holder(design)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_LIKE_CHANGED)) {
            val item = dataList[position]
            applyLikeUi(holder, item.isLiked, playAnim = false)
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = dataList[position]

        holder.userName.text = item.userName
        holder.location.text = item.location

        Glide.with(holder.itemView)
            .load(item.imageUrl)
            .placeholder(R.drawable.secondimg)
            .error(R.drawable.secondimg)
            .into(holder.sharedImg)

        applyLikeUi(holder, item.isLiked, playAnim = false)

        holder.likeBtn.setOnClickListener {
            toggleLike(holder, holder.bindingAdapterPosition, playAnim = true)
        }

        holder.commentBtn.setOnClickListener {
            onCommentClick(item, holder.bindingAdapterPosition)
        }

        val detector = GestureDetector(
            mContext,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean = true
                override fun onDown(e: MotionEvent): Boolean = true
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val now = SystemClock.elapsedRealtime()
                    if (now - holder.lastDoubleTapTs < 250) return true
                    holder.lastDoubleTapTs = now
                    toggleLike(holder, holder.bindingAdapterPosition, playAnim = true)
                    return true
                }
            }
        )
        holder.bindDoubleTap(detector)
    }

    private fun toggleLike(holder: Holder, position: Int, playAnim: Boolean) {
        if (position == RecyclerView.NO_POSITION) return
        val item = dataList[position]
        item.isLiked = !item.isLiked

        applyLikeUi(holder, item.isLiked, playAnim)
        onLikeChanged(item, position, item.isLiked)

        notifyItemChanged(position, PAYLOAD_LIKE_CHANGED)
    }

    private fun applyLikeUi(holder: Holder, isLiked: Boolean, playAnim: Boolean) {
        if (isLiked) {
            holder.likeBtn.setBackgroundResource(R.drawable.fullhearth)
            if (playAnim) holder.playHeartAnim()
        } else {
            holder.likeBtn.setBackgroundResource(R.drawable.heart)
        }
    }
}
