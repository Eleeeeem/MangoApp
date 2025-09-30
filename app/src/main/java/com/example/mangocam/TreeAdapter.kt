package com.example.mangocam.ui.logs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.R
import com.example.mangocam.model.Farm
import com.example.mangocam.model.Tree
import com.example.mangocam.utils.Constant
import com.example.mangocam.utils.PlantDescriptionCreator
import com.example.mangoo.DiseaseHistory
import com.example.mangoo.DiseaseSuggestion
import com.example.mangoo.PlantResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TreeAdapter(
    private val trees: MutableList<Tree>,
    private val onClick: (Tree) -> Unit,
    private val onLongClick: (Tree) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {

    private val selectedTrees = mutableSetOf<Tree>()
    val gson = Gson()

    inner class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tvTreeId)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(tree: Tree) {

            if(tree.data != null)
            {
                val type = object : TypeToken<PlantResponse>() {}.type
                val treeDetails: PlantResponse? = gson.fromJson(tree.data, type)

//                val suggestions = treeDetails?.result?.classification?.suggestions
//                val topSuggestion = suggestions?.firstOrNull()
//                val rawPlantName = topSuggestion?.name ?: "Unknown Plant"
//                val formattedPlantName =
//                    if (rawPlantName.lowercase().contains("mangifera")) "Mangifera indica" else rawPlantName

                val diseaseSuggestions = treeDetails?.result?.disease?.suggestions
                    ?: treeDetails?.health_assessment?.diseases

                var resultStatus = ""
                if (diseaseSuggestions.isNullOrEmpty()) {
                    resultStatus = "✅ Looks healthy!"
                } else {

                    val detail = PlantDescriptionCreator.showDiseaseDetails(diseaseSuggestions, "")
                    val diseaseName = detail.diseaseName
                    resultStatus = """
                        Not Healthy
                        🦠 Disease: $diseaseName
                        """.trimIndent()
                }

                tvStatus.text = resultStatus

            }else{
                tvStatus.text = "Not Yet Checked"
            }

            tvId.text = tree.name
            itemView.isSelected = selectedTrees.contains(tree)

            itemView.setOnClickListener {
                if (selectedTrees.isNotEmpty()) {
                    toggleSelection(tree)
                } else {
                    onClick(tree)
                }
            }

            itemView.setOnLongClickListener {
                toggleSelection(tree)
                onLongClick(tree)
                true
            }
        }

        private fun toggleSelection(tree: Tree) {
            if (selectedTrees.contains(tree)) {
                selectedTrees.remove(tree)
            } else {
                selectedTrees.add(tree)
            }
            notifyItemChanged(adapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tree, parent, false)
        return TreeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        holder.bind(trees[position])
    }

    override fun getItemCount(): Int = trees.size

    // Now returns full Tree objects
    fun getSelectedTrees(): List<Tree> = selectedTrees.toList()

    fun clearSelection() {
        selectedTrees.clear()
        notifyDataSetChanged()
    }

    fun addTree(tree: Tree) {
        trees.add(tree) //
        notifyItemInserted(trees.size - 1)
    }
}
