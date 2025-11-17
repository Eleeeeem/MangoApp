package com.example.mangoo

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangocam.R

class HistoryAdapter(
    private val historyList: List<DiseaseHistory>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivDiseaseImage: ImageView = view.findViewById(R.id.ivDiseaseImage)
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

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]

        // Load image
        if (!item.imageUri.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(Uri.parse(item.imageUri))
                .placeholder(R.drawable.ic_placeholder)
                .into(holder.ivDiseaseImage)
        } else {
            holder.ivDiseaseImage.setImageResource(R.drawable.ic_placeholder)
        }

        // Disease name and accuracy
        holder.tvDiseaseName.text = "🦠 ${item.diseaseName ?: "Unknown Disease"}"
        holder.tvAccuracy.text = "📊 Accuracy: ${item.accuracy ?: "N/A"}"
        holder.tvDate.text = "🕒 ${item.date ?: "Unknown"}"

        // Build treatment string
        val sb = StringBuilder()

        val bio = item.biologicalTreatment ?: emptyList()
        val chem = item.chemicalTreatment ?: emptyList()
        val prev = item.prevention ?: emptyList()

        sb.append("🧬 Biological Treatment:\n")
        sb.append(if (bio.isNotEmpty()) bio.joinToString("\n") { "• $it" } else "No Data was added.")
        sb.append("\n\n")

        sb.append("🧪 Chemical Treatment:\n")
        sb.append(if (chem.isNotEmpty()) chem.joinToString("\n") { "• $it" } else "No Data was added.")
        sb.append("\n\n")

        sb.append("🛡️ Prevention:\n")
        sb.append(if (prev.isNotEmpty()) prev.joinToString("\n") { "• $it" } else "No Data was added.")

        holder.tvTreatment.text = sb.toString()

        // Click listener → open DiseaseDetailActivity
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DiseaseDetailActivity::class.java).apply {
                putExtra("diseaseName", item.diseaseName)
                putExtra("accuracy", item.accuracy)
                putExtra("treatment", sb.toString())
                putExtra("date", item.date)
                putExtra("imageUri", item.imageUri)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = historyList.size
}
