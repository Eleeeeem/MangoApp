package com.example.mangoo

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.R

class HistoryAdapter(
    private val historyList: List<DiseaseHistory>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlantName: TextView = view.findViewById(R.id.tvPlantName)
        val tvDiseaseName: TextView = view.findViewById(R.id.tvDiseaseName)
        val tvAccuracy: TextView = view.findViewById(R.id.tvAccuracy)
        val tvTreatment: TextView = view.findViewById(R.id.tvTreatment)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    // ✅ Put the code you showed me HERE
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]

        holder.tvPlantName.text = "🌱 Plant: ${item.plantName}"
        holder.tvDiseaseName.text = "🦠 Disease: ${item.diseaseName}"
        holder.tvAccuracy.text = "📊 Accuracy: ${item.accuracy}"
        holder.tvTreatment.text = "💊 Treatment: ${item.treatment ?: "No treatment info"}"
        holder.tvDate.text = "🕒 ${item.date}"

        // ✅ Highlight Healthy vs Diseased
        if (item.diseaseName.equals("Healthy", ignoreCase = true)) {
            holder.tvDiseaseName.setTextColor(Color.parseColor("#388E3C")) // Green
        } else {
            holder.tvDiseaseName.setTextColor(Color.parseColor("#D32F2F")) // Red
        }
    }

    override fun getItemCount(): Int = historyList.size
}
