package com.example.anikutusu.ui.badges

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.anikutusu.R
import com.example.anikutusu.databinding.ItemBadgeBinding
import com.example.anikutusu.model.Badge

class BadgeAdapter(
    private val onClick: (Badge) -> Unit
) : ListAdapter<Badge, BadgeAdapter.BadgeViewHolder>(BadgeDiff()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        // id yoksa pozisyon fallback (ama modelde id zorunlu tuttuk)
        return getItem(position).id.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BadgeViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BadgeViewHolder(
        private val b: ItemBadgeBinding,
        private val onClick: (Badge) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(badge: Badge) {
            // --- Texts
            b.badgeName.text = badge.name
            // (İstersen tooltip ya da secondary text için description da gösterebilirsin)
            // b.badgeDesc.text = badge.description

            // --- Icon (Glide)
            if (badge.iconUrl.isNotBlank()) {
                Glide.with(b.badgeIcon.context)
                    .load(badge.iconUrl)
                    .placeholder(R.drawable.photo)
                    .error(R.drawable.loki)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(b.badgeIcon)
            } else {
                b.badgeIcon.setImageResource(R.drawable.loki)
            }

            // --- Lock/Grayscale
            if (badge.unlocked) {
                b.lockOverlay.visibility = View.GONE

                // alpha ve filter’ı tam sıfırla (recycling koruması)
                b.badgeIcon.imageAlpha = 255
                b.badgeIcon.colorFilter = null
            } else {
                b.lockOverlay.visibility = View.VISIBLE

                b.badgeIcon.imageAlpha = 120
                val cm = ColorMatrix().apply { setSaturation(0f) }
                b.badgeIcon.colorFilter = ColorMatrixColorFilter(cm)
            }

            // --- Progress (varsa göster)
            // Eğer layout’ta progress bar/text eklediysen:
            // Örn: b.progressBar ve b.progressText
            badge.progress?.let { p ->
                if (!badge.unlocked) {
                    b.progressBar?.visibility = View.VISIBLE
                    b.progressText?.visibility = View.VISIBLE
                    b.progressBar?.progress = p.coerceIn(0, 100)
                    b.progressText?.text = "%$p"
                } else {
                    b.progressBar?.visibility = View.GONE
                    b.progressText?.visibility = View.GONE
                }
            } ?: run {
                // progress null ise tamamen gizle
                b.progressBar?.visibility = View.GONE
                b.progressText?.visibility = View.GONE
            }

            // --- Click
            b.root.setOnClickListener { onClick(badge) }
        }
    }

    class BadgeDiff : DiffUtil.ItemCallback<Badge>() {
        override fun areItemsTheSame(oldItem: Badge, newItem: Badge) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Badge, newItem: Badge) = oldItem == newItem
    }
}
