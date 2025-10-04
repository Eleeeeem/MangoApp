package com.example.mangocam.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.R
import com.example.mangoo.DiseaseSuggestion

class TreeHistoryAdapter(private val diseases: List<DiseaseSuggestion>, val dateScanned : String): RecyclerView.Adapter<TreeHistoryAdapter.HistoryViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_treehistory, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: HistoryViewHolder,
        position: Int
    ) {
        val item = diseases[position]
        val accuracy = String.format("%.2f", item.probability!! * 100)

        val biologicalTreatment = item.details.treatment.biological?.joinToString("\n\n") ?: ""
        val chemicalTreatment = item.details.treatment.chemical?.joinToString("\n\n") ?: ""
        val prevention = item.details.treatment.prevention?.joinToString("\n\n") ?: ""

        val biologicalList = item.details.treatment.biological?: emptyList()
        val chemicalList = item.details.treatment.chemical ?: emptyList()
        val preventionList = item.details.treatment.prevention ?: emptyList()

        val sb = StringBuilder()

        if (biologicalList.isNotEmpty()) {
            sb.append("🧬 Biological Treatment:\n")
            sb.append(biologicalList.joinToString("\n") { "• $it" })
            sb.append("\n\n")
        }else
        {
            sb.append("🧬 Biological Treatment:\n")
            sb.append("No Date was added.")
            sb.append("\n\n")
        }

        if (chemicalList.isNotEmpty()) {
            sb.append("🧪 Chemical Treatment:\n")
            sb.append(chemicalList.joinToString("\n") { "• $it" })
            sb.append("\n\n")
        }else
        {
            sb.append("🧪 Chemical Treatment:\n")
            sb.append("No Date was added.")
            sb.append("\n\n")
        }

        if (preventionList.isNotEmpty()) {
            sb.append("🛡️ Prevention:\n")
            sb.append(preventionList.joinToString("\n") { "• $it" })
        }else
        {
            sb.append("🛡️ Prevention:\n")
            sb.append("No Date was added.")
        }

        val treatmentText = sb.toString().trim()
        holder.tvTreatment.text = treatmentText

        holder.tvAccuracy.text = "📊 Accuracy: ${accuracy ?: "No accuracy info"}"
        holder.tvDate.text = "🕒 Scanned Date :  ${dateScanned}"
        holder.tvDiseaseName.text = "🦠 Disease: ${item.name}"
    }

    override fun getItemCount(): Int = diseases.size

    inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDiseaseName: TextView = view.findViewById(R.id.tvDiseaseName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvAccuracy: TextView = view.findViewById(R.id.tvAccuracy)
        val tvTreatment: TextView = view.findViewById(R.id.tvTreatment)
    }
}