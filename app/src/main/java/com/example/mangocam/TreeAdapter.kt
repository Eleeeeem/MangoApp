package com.example.mangocam.ui.logs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.R
import com.example.mangocam.model.Farm
import com.example.mangocam.model.Tree
import com.example.mangocam.utils.Constant
import com.example.mangoo.PlantResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TreeAdapter(
    private val trees: MutableList<Tree>,
    private val onDiagnoseClick: (Tree) -> Unit,
    private val onCheckDetailClick: (Tree) -> Unit,
    private val onDeleteClick: (Tree) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {

    private val gson = Gson()

    inner class TreeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tvTreeId)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val checkDetail: MaterialButton = itemView.findViewById(R.id.checkDetail)
        private val diagnose: MaterialButton = itemView.findViewById(R.id.diagnose)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteTree)
        private val renameButton: ImageButton = itemView.findViewById(R.id.btnRenameTree)

        fun bind(tree: Tree) {
            tvId.text = tree.name
            tvStatus.text = if (tree.data == null) "Not Yet Checked" else "Checked"

            diagnose.setOnClickListener { onDiagnoseClick(tree) }
            checkDetail.setOnClickListener { onCheckDetailClick(tree) }

            // 🔹 Delete just calls callback to activity
            deleteButton.setOnClickListener {
                android.util.Log.d("TreeAdapter", "Adapter delete clicked for ${tree.name}")
                onDeleteClick(tree)
            }

            // ✏️ Rename
            renameButton.setOnClickListener {
                showRenameDialog(itemView.context, tree, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tree, parent, false)
        return TreeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        holder.bind(trees[position])
    }

    override fun getItemCount(): Int = trees.size

    fun addTrees(newTrees: List<Tree>) {
        val start = trees.size
        trees.addAll(newTrees)
        notifyItemRangeInserted(start, newTrees.size)
    }

    private fun showRenameDialog(context: Context, tree: Tree, position: Int) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(tree.name)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)   // optional, for hint
        }

        val dialog = MaterialAlertDialogBuilder(context, R.style.MangoDialogStyle)
            .setTitle("✏️ Rename Tree")
            .setView(input)
            .setPositiveButton("Save") { d, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    tree.name = newName
                    notifyItemChanged(position)
                    updateTreeNameInStorage(context, tree)
                    Toast.makeText(context, "Renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.show()

        // ✅ Apply black text colors for title and buttons
        dialog.apply {
            findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
        }
    }


    private fun updateTreeNameInStorage(context: Context, updatedTree: Tree) {
        val sharedPref = context.getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val type = object : TypeToken<MutableList<Farm>>() {}.type
        val farmList: MutableList<Farm> =
            gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()
        for (farm in farmList) {
            val targetTree = farm.trees.find { it.id == updatedTree.id }
            if (targetTree != null) {
                targetTree.name = updatedTree.name
                break
            }
        }
        sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()
    }
}
