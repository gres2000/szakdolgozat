package com.taskraze.myapplication.viewmodel.recommendation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.databinding.ItemRecommendationBinding
import com.taskraze.myapplication.view.recommendation.Recommendation

class CustomRecommendationsAdapter(
private var items: List<Recommendation>
) : RecyclerView.Adapter<CustomRecommendationsAdapter.RecommendationViewHolder>() {

    inner class RecommendationViewHolder(val binding: ItemRecommendationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val binding = ItemRecommendationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecommendationViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        val item = items[position]
        holder.binding.recommendationTitle.text = item.title
        holder.binding.recommendationTags.text = item.tags.joinToString(", ")
        holder.binding.recommendationType.text = item.type

        holder.itemView.setOnClickListener {
            item.url?.let { url ->
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse(url)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    fun updateItems(newItems: List<Recommendation>) {
        items = newItems
        notifyDataSetChanged()
    }
}