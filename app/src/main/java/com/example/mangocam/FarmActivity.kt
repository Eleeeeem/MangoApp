package com.example.mangocam

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.model.Farm
import com.example.mangocam.model.Tree
import com.example.mangocam.ui.logs.TreeAdapter
import com.example.mangocam.utils.Constant
import com.example.mangoo.PlantResponse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FarmActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private val gson = Gson()
    private lateinit var rcTrees: RecyclerView
    private lateinit var adapter: TreeAdapter
    private lateinit var titleTv: TextView
    private lateinit var selectedTree: Tree

    private lateinit var farm: Farm
    private var trees: MutableList<Tree> = mutableListOf()

    // Debounce flags to prevent multiple dialogs
    private var isDeletingTree = false
    private var isAddingTree = false
    private var isRemovingFarm = false
    private var userId: String? = null
    private lateinit var loadingOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_farm)

        firestore = FirebaseFirestore.getInstance()
        val sharedPref = this.getSharedPreferences(Constant.SHARED_PREF_USER, Context.MODE_PRIVATE)
        userId = sharedPref.getString(Constant.SHARED_PREF_USER_DETAIL_USERID, null)

        rcTrees = findViewById(R.id.rcTrees)
        titleTv = findViewById(R.id.titleTv)
        loadingOverlay = findViewById(R.id.loadingOverlay)

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

            if(selectedTree.data == null)
            {
                selectedTree.data =  mutableListOf()
            }

            selectedTree.data!!.add(plantDetail!!)
                //gson.toJson(plantDetail)
            lifecycleScope.launch {
                updateTreeData(selectedTree)
            }
        }
    }

    private fun   getIntentData() {
        farm = intent.getSerializableExtra("farm") as Farm
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
            },
            onRenameClick = { tree ->
                showRenameDialog(tree)
            }
        )
        rcTrees.adapter = adapter
    }

    private fun showRenameDialog(tree: Tree) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(tree.name)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)   // optional, for hint
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.MangoDialogStyle)
            .setTitle("✏️ Rename Tree")
            .setView(input)
            .setPositiveButton("Save") { d, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    tree.name = newName
                    lifecycleScope.launch {
                        showLoading()

                        firestore.collection("users")
                            .document(userId!!)
                            .collection("farms")
                            .document(farm.id)
                            .collection("trees")
                            .document(tree.baseId!!)
                            .update("name", newName)
                            .await()

                        adapter.notifyDataSetChanged()
                        hideLoading()

                        Toast.makeText(this@FarmActivity, "Renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                    }
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

    private suspend fun updateTreeData(updatedTree: Tree) {
        showLoading()
        firestore.collection("users")
            .document(userId!!)
            .collection("farms")
            .document(farm.id)
            .collection("trees")
            .document(updatedTree.baseId!!)
            .update("data", updatedTree.data)
            .await()

        adapter.notifyDataSetChanged()

        hideLoading()
    }

    /**
     * 🗑️ Delete Tree Confirmation Dialog
     */
    private fun showDeleteTreeDialog(tree: Tree) {

        val dialog = MaterialAlertDialogBuilder(this, R.style.MangoDialogStyle)
            .setTitle("🗑️ Remove Tree")
            .setMessage("Are you sure you want to remove \"${tree.name}\"? This action cannot be undone.")
            .setPositiveButton("Yes, Remove") { dialogInterface, _ ->
                lifecycleScope.launch {
                    deleteTree(tree)
                    Toast.makeText(
                        this@FarmActivity,
                        "\"${tree.name}\" removed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialogInterface.dismiss()
                }
            }
            .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
            .create()

        dialog.show()
        dialog.apply {
            findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(Color.BLACK)
            findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
        }
    }



    private suspend fun deleteTree(tree: Tree) {
        loadingOverlay.visibility = View.VISIBLE
        val treeDocRef = firestore.collection("users")
            .document(userId!!)
            .collection("farms")
            .document(farm.id)
            .collection("trees")
            .document(tree.baseId!!)

        treeDocRef.delete().await()
        trees.removeIf { it.baseId == tree.baseId }

        adapter.notifyDataSetChanged()
        loadingOverlay.visibility = View.GONE
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

                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val startId = (trees.maxOfOrNull { it.id } ?: 0) + 1

                val newTrees = (0 until count).map { index ->
                    val newId = startId + index
                    Tree(
                        name = "Tree $newId",
                        id = newId,
                        baseId = UUID.randomUUID().toString(),
                        plantedDate = currentDate,
                        status = "Newly Planted",
                        data = mutableListOf()
                    )
                }

                lifecycleScope.launch {
                    try {
                        val batch = firestore.batch()
                        val treesCollection = firestore.collection("users")
                            .document(userId!!)
                            .collection("farms")
                            .document(farm.id)
                            .collection("trees")

                        for (tree in newTrees) {
                            val treeDoc = treesCollection.document(tree.baseId!!)
                            batch.set(treeDoc, tree)
                        }

                        batch.commit().await() // Wait until all trees are saved
                        adapter.addTrees(newTrees) // Update UI after successful commit
                        Toast.makeText(this@FarmActivity, "Trees added successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@FarmActivity, "Failed to add trees: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isAddingTree = false
                        d.dismiss()
                    }
                }

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

    private fun hideLoading()
    {
        loadingOverlay.visibility = View.GONE
    }

    private fun showLoading()
    {
        loadingOverlay.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }


}
