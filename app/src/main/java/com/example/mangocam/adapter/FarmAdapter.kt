package com.example.mangocam.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
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
import com.example.mangocam.utils.Constant
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale

class FarmAdapter(
    private val farms: MutableList<Farm>,
    private val onClick: (Farm) -> Unit,
    private val onDelete: (Farm) -> Unit
) : RecyclerView.Adapter<FarmAdapter.FarmViewHolder>() {

    private val gson = Gson()
    private var lastDeleteClick = 0L // debounce timestamp

    inner class FarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tvTreeId)
        private val tvTreeCount: TextView = itemView.findViewById(R.id.tvTreeCount)
        private val tvSprayDate: TextView = itemView.findViewById(R.id.tvFirstSprayDate)
        private val renameButton: ImageButton = itemView.findViewById(R.id.btnRenameFarm)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteFarm)
        private var isDeleting = false // prevent multiple dialogs

        fun bind(farm: Farm) {

            tvName.text = farm.name
            tvTreeCount.text = "Tree Count: ${farm.trees.size}"

            tvSprayDate.text = farm.sprayDate?.let {
                try {
                    val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val output = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                    "First Spray: ${output.format(input.parse(it)!!)}"
                } catch (e: Exception) {
                    "First Spray: Unknown"
                }
            } ?: "First Spray: Not yet set"

            itemView.setOnClickListener { onClick(farm) }

            renameButton.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    showRenameDialog(itemView.context, farm, pos)
                }
            }

            deleteButton.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastDeleteClick < 500 || isDeleting) return@setOnClickListener
                lastDeleteClick = now
                isDeleting = true

                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    showDeleteConfirmation(itemView.context, farm)
                } else {
                    isDeleting = false
                }
            }
        }

        private fun showDeleteConfirmation(context: Context, farm: Farm) {
            val dialog = MaterialAlertDialogBuilder(context, R.style.MangoDialogStyle)
                .setTitle("🗑️ Delete Farm")
                .setMessage("Are you sure you want to delete \"${farm.name}\"? This action cannot be undone.")
                .setPositiveButton("Delete") { d, _ ->
                    onDelete(farm)
                    Toast.makeText(context, "\"${farm.name}\" deleted.", Toast.LENGTH_SHORT).show()
                    isDeleting = false
                    d.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ ->
                    isDeleting = false
                    d.dismiss()
                }
                .create()

            dialog.show()

            dialog.apply {
                findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(Color.BLACK)
                findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_farm, parent, false)
        return FarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: FarmViewHolder, position: Int) {
        holder.bind(farms[position])
    }

    override fun getItemCount(): Int = farms.size

    // ---------------------- RENAME DIALOG ----------------------
    private fun showRenameDialog(context: Context, farm: Farm, position: Int) {
        val input = EditText(context).apply {
            hint = "Enter new farm name"
            setHintTextColor(Color.parseColor("#808080"))
            setTextColor(Color.BLACK)
            setText(farm.name)
            setPadding(60, 50, 60, 50)
        }

        val dialog = MaterialAlertDialogBuilder(context, R.style.MangoDialogStyle)
            .setTitle("✏️ Rename Farm")
            .setMessage("Give your farm a new name:")
            .setView(input)
            .setPositiveButton("Save") { d, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    farm.name = newName
                    updateFarmInStorage(context, farm)
                    notifyItemChanged(position)
                    Toast.makeText(context, "Farm renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.show()

        dialog.apply {
            findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(Color.BLACK)
            findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
        }
    }

    // ---------------------- UPDATE STORAGE ----------------------
    private fun updateFarmInStorage(context: Context, updatedFarm: Farm) {
        val sharedPref = context.getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val json = sharedPref.getString(Constant.SHARED_PREF_FARM, null)
        val type = object : TypeToken<MutableList<Farm>>() {}.type
        val farmList: MutableList<Farm> = gson.fromJson(json, type) ?: mutableListOf()

        val index = farmList.indexOfFirst { it.id == updatedFarm.id }
        if (index != -1) farmList[index].name = updatedFarm.name

        sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()
    }
}
