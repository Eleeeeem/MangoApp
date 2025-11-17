package com.example.mangocam.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangocam.R
import com.example.mangoo.DiseaseSuggestion

// Top-level helper data class for grouped history items
data class HistoryEntry(
    val group: String,
    val suggestion: DiseaseSuggestion,
    val date: String,
    val imageUri: String? // PlantResponse.imageUri (stable) — may be null
)

class TreeHistoryAdapter(
    private val items: List<Item>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1

        // Build adapter from HistoryEntry list (already grouped)
        fun withGroups(data: List<HistoryEntry>): TreeHistoryAdapter {
            val grouped = mutableListOf<Item>()
            var currentHeader: String? = null
            for (entry in data) {
                if (entry.group != currentHeader) {
                    grouped.add(Item.Header(entry.group))
                    currentHeader = entry.group
                }
                grouped.add(Item.Data(entry.suggestion, entry.date, entry.imageUri))
            }
            return TreeHistoryAdapter(grouped)
        }
    }

    sealed class Item {
        data class Header(val title: String) : Item()
        data class Data(
            val suggestion: DiseaseSuggestion,
            val date: String,
            val imageUri: String?
        ) : Item()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Item.Header -> TYPE_HEADER
            is Item.Data -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_treehistory, parent, false)
            HistoryViewHolder(view)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Item.Header -> (holder as HeaderViewHolder).bind(item)
            is Item.Data -> (holder as HistoryViewHolder).bind(item)
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val headerText: TextView = view.findViewById(R.id.tvHeader)
        fun bind(item: Item.Header) {
            headerText.text = "📅 ${item.title}"
        }
    }

    inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val ivDiseaseImage: ImageView = view.findViewById(R.id.ivDiseaseImage)
        private val tvDiseaseName: TextView = view.findViewById(R.id.tvDiseaseName)
        private val tvDate: TextView = view.findViewById(R.id.tvDate)
        private val tvAccuracy: TextView = view.findViewById(R.id.tvAccuracy)
        private val tvTreatment: TextView = view.findViewById(R.id.tvTreatment)

        fun bind(item: Item.Data) {
            val suggestion = item.suggestion
            val accuracy = String.format("%.2f", (suggestion.probability ?: 0.0) * 100)

            // 👉 TREATMENT STRING BUILDER
            val bio = suggestion.details.treatment.biological ?: emptyList()
            val chem = suggestion.details.treatment.chemical ?: emptyList()
            val prev = suggestion.details.treatment.prevention ?: emptyList()

            val sb = StringBuilder()

            if (bio.isNotEmpty()) sb.append("🧬 Biological Treatment:\n${bio.joinToString("\n") { "• $it" }}\n\n")
            else sb.append("🧬 Biological Treatment:\nNo Data was added.\n\n")

            if (chem.isNotEmpty()) sb.append("🧪 Chemical Treatment:\n${chem.joinToString("\n") { "• $it" }}\n\n")
            else sb.append("🧪 Chemical Treatment:\nNo Data was added.\n\n")

            if (prev.isNotEmpty()) sb.append("🛡️ Prevention:\n${prev.joinToString("\n") { "• $it" }}")
            else sb.append("🛡️ Prevention:\nNo Data was added.")

            // 📌 TEXT FIELDS
            tvDiseaseName.text = "🦠 Disease: ${suggestion.name}"
            tvAccuracy.text = "📊 Accuracy: $accuracy%"
            tvDate.text = "🕒 Scanned Date: ${item.date}"
            tvTreatment.text = sb.toString()

            // 📸 Load image from suggestion.imageUri first, fallback to item.imageUri (PlantResponse.imageUri)
            val uriString = suggestion.imageUri ?: item.imageUri ?: ""

            if (uriString.isNotEmpty()) {
                ivDiseaseImage.visibility = View.VISIBLE

                // Glide accepts String or Uri — pass String directly
                Glide.with(ivDiseaseImage.context)
                    .load(uriString)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(ivDiseaseImage)
            } else {
                ivDiseaseImage.visibility = View.GONE
            }
        }
    }
}
