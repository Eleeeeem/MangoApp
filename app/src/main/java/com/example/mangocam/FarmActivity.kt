package com.example.mangocam

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.model.Farm
import com.example.mangocam.model.Tree
import com.example.mangocam.ui.logs.TreeAdapter
import com.example.mangocam.utils.Constant
import com.example.mangoo.PlantResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class FarmActivity : AppCompatActivity() {

    private val gson = Gson()
    private lateinit var rcTrees: RecyclerView
    private lateinit var adapter: TreeAdapter
    private lateinit var titleTv: TextView
    private lateinit var selectedTree: Tree

    private var farm: Farm? = null
    private var trees: MutableList<Tree> = mutableListOf()

    // Debounce flags to prevent multiple dialogs
    private var isDeletingTree = false
    private var isAddingTree = false
    private var isRemovingFarm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_farm)

        rcTrees = findViewById(R.id.rcTrees)
        titleTv = findViewById(R.id.titleTv)

        getIntentData()
        setUpTrees()

        titleTv.text = farm?.name ?: "My Farm"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private val detailResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val plantDetail = data?.getSerializableExtra("plantDetail") as? PlantResponse
            selectedTree.data = gson.toJson(plantDetail)
            updateTreeData(selectedTree)
        }
    }

    private fun getIntentData() {
        farm = intent.getSerializableExtra("farm") as? Farm
        trees = farm?.trees?.toMutableList() ?: mutableListOf()
        Log.d("FarmActivity", "Loaded farm: ${farm?.name}")
    }

    private fun setUpTrees() {
        rcTrees.layoutManager = GridLayoutManager(this, 1)
        adapter = TreeAdapter(
            trees,
            onDiagnoseClick = { tree ->
                selectedTree = tree
                val intent = Intent(this, PlantScanActivity::class.java)
                detailResultLauncher.launch(intent)
            },
            onCheckDetailClick = { tree ->
                if (tree.data == null) {
                    Toast.makeText(this, "Diagnose first to add history.", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, TreeHistoryActivity::class.java)
                    intent.putExtra("tree", tree)
                    detailResultLauncher.launch(intent)
                }
            },
            onDeleteClick = { tree ->
                if (!isDeletingTree) showDeleteTreeDialog(tree)
            }
        )
        rcTrees.adapter = adapter
    }

    private fun updateTreeData(updatedTree: Tree) {
        val sharedPref = getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val type = object : TypeToken<MutableList<Farm>>() {}.type
        val farmList: MutableList<Farm> =
            gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

        val targetFarm = farmList.find { it.id == farm?.id }
        val targetTree = targetFarm?.trees?.find { it.id == updatedTree.id }
        targetTree?.data = updatedTree.data

        sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()
        setUpTrees()
    }

    /**
     * 🗑️ Delete Tree Confirmation Dialog
     */
    private fun showDeleteTreeDialog(tree: Tree) {
        android.util.Log.d("FarmActivity", "Activity delete dialog triggered for ${tree.name}")

        val dialog = MaterialAlertDialogBuilder(this, R.style.MangoDialogStyle)
            .setTitle("🗑️ Remove Tree")
            .setMessage("Are you sure you want to remove \"${tree.name}\"? This action cannot be undone.")
            .setPositiveButton("Yes, Remove") { dialogInterface, _ ->
                deleteTree(tree)
                Toast.makeText(this, "\"${tree.name}\" removed.", Toast.LENGTH_SHORT).show()
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()  // create first

        dialog.show() // then show

        // ✅ Apply black text colors
        dialog.apply {
            findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(Color.BLACK)
            findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
        }
    }



    private fun deleteTree(tree: Tree) {
        val sharedPref = getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val type = object : TypeToken<MutableList<Farm>>() {}.type
        val farmList: MutableList<Farm> =
            gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

        val targetFarm = farmList.find { it.id == farm?.id }
        targetFarm?.trees?.removeIf { it.id == tree.id }

        sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()
        trees.removeIf { it.id == tree.id }
        adapter.notifyDataSetChanged()
    }

    /**
     * 🌳 Add Tree Dialog (Modern Material UI)
     */
    fun AddTree(view: View) {
        if (isAddingTree) return
        isAddingTree = true

        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Enter number of trees"
            setHintTextColor(Color.parseColor("#808080"))
            setTextColor(Color.BLACK)
            setText(hint)
            setPadding(60, 50, 60, 50)
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.MangoDialogStyle)
            .setTitle("🌳 Add Trees")
            .setMessage("How many trees would you like to add manually?")
            .setView(input)
            .setPositiveButton("Add") { d, _ ->
                val count = input.text.toString().toIntOrNull()
                if (count == null || count <= 0) {
                    Toast.makeText(this, "Please enter a valid number.", Toast.LENGTH_SHORT).show()
                    isAddingTree = false
                    return@setPositiveButton
                }

                val sharedPref = getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
                val type = object : TypeToken<MutableList<Farm>>() {}.type
                val farmList: MutableList<Farm> =
                    gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

                val targetFarm = farmList.find { it.id == farm?.id }
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val startId = (targetFarm?.trees?.maxOfOrNull { it.id } ?: 0) + 1

                val newTrees = (0 until count).map { index ->
                    val newId = startId + index
                    Tree(
                        name = "Tree $newId",
                        id = newId,
                        plantedDate = currentDate,
                        status = "Newly Planted",
                        data = null
                    )
                }

                targetFarm?.trees?.addAll(newTrees)
                sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()

                adapter.addTrees(newTrees)
                Toast.makeText(this, "${newTrees.size} trees added!", Toast.LENGTH_SHORT).show()
                isAddingTree = false
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ ->
                isAddingTree = false
                d.dismiss()
            }
            .create()

        dialog.show() // then show

        // ✅ Apply black text colors
        dialog.apply {
            findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(Color.BLACK)
            findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
        }
    }
    /**
     * 🏡 Remove Farm Confirmation (Modern, Single Dialog)
     */
    fun showDeleteFarmDialog() {
        farm?.let { targetFarm ->
            android.util.Log.d("FarmActivity", "Activity delete dialog triggered for farm: ${targetFarm.name}")

            MaterialAlertDialogBuilder(this, R.style.MangoDialogStyle)
                .setTitle("🏡 Remove Farm")
                .setMessage("Are you sure you want to permanently delete \"${targetFarm.name}\" and all its trees?")
                .setPositiveButton("Yes, Remove") { dialog, _ ->
                    removeFarmConfirmed(targetFarm)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    Toast.makeText(this, "Farm removal canceled", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun removeFarmConfirmed(targetFarm: Farm) {
        val sharedPref = getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val type = object : TypeToken<MutableList<Farm>>() {}.type
        val farmList: MutableList<Farm> =
            Gson().fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

        farmList.removeIf { it.id == targetFarm.id }
        sharedPref.edit().putString(Constant.SHARED_PREF_FARM, Gson().toJson(farmList)).apply()

        Toast.makeText(this, "Farm \"${targetFarm.name}\" removed!", Toast.LENGTH_SHORT).show()
        android.util.Log.d("FarmActivity", "Farm ${targetFarm.name} deleted successfully")

        setResult(Activity.RESULT_OK)
        finish()
    }

}
