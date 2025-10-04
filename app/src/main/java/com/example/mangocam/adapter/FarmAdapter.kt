package com.example.mangocam.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.R
import com.example.mangocam.model.Farm
import com.example.mangocam.model.Tree
import java.text.SimpleDateFormat
import java.util.Locale

class FarmAdapter(
    private val farms: List<Farm>,
    private val onClick: (Farm) -> Unit) : RecyclerView.Adapter<FarmAdapter.FarmViewHolder>() {

    private val selectedFarm = mutableSetOf<Farm>()

    inner class FarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tvTreeId)
        private val tvTreeCount: TextView = itemView.findViewById(R.id.tvTreeCount)
        private val tvCreatedDate: TextView = itemView.findViewById(R.id.tvCreatedDate)

        fun bind(farm: Farm) {
            tvId.text = farm.name
            tvTreeCount.text = "Tree Count: ${farm.trees.size}"

            var newFormatDate : String = ""
            if(farm.sprayDate == null)
            {
                newFormatDate = "Not yet set"
            }else{
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                val parsedDate = inputFormat.parse(farm.sprayDate)
                newFormatDate = parsedDate?.let { outputFormat.format(it) } ?: "Unknown"
            }

            tvCreatedDate.text = newFormatDate

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