package com.example.mangocam.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.R
import com.example.mangocam.model.Farm

class FarmAdapter(
    private val farms: List<Farm>,
    private val onClick: (Farm) -> Unit) : RecyclerView.Adapter<FarmAdapter.FarmViewHolder>() {

    inner class FarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tvTreeId)
        private val tvTreeCount: TextView = itemView.findViewById(R.id.tvTreeCount)
        private val tvCreatedDate: TextView = itemView.findViewById(R.id.tvCreatedDate)

        fun bind(farm: Farm) {
            tvId.text = farm.name
            tvTreeCount.text = "Tree Count: ${farm.trees.size}"

            tvCreatedDate.text = farm.sprayDate?.let { "Spray Date: $it" } ?: "Not yet set"

            itemView.setOnClickListener {
                onClick(farm)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_farm, parent, false)
        return FarmViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: FarmViewHolder,
        position: Int
    ) {
        holder.bind(farms[position])
    }

    override fun getItemCount(): Int = farms.size
}