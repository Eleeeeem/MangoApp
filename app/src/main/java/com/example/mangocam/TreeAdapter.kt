package com.example.mangocam.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.R
import com.example.mangocam.model.Tree

class TreeAdapter(
    private val trees: MutableList<Tree>,
    private val onClick: (Tree) -> Unit,
    private val onLongClick: (Tree) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {

    private val selectedTrees = mutableSetOf<Tree>()

    inner class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tvTreeId)

        fun bind(tree: Tree) {
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
