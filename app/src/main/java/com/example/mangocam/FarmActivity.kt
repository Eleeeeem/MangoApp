package com.example.mangocam

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.adapter.FarmAdapter
import com.example.mangocam.model.Farm
import com.example.mangocam.model.Tree
import com.example.mangocam.ui.logs.TreeAdapter
import com.example.mangocam.utils.Constant
import com.example.mangoo.DiseaseHistory
import com.example.mangoo.PlantResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FarmActivity : AppCompatActivity() {

    val gson = Gson()

    private lateinit var rcTrees: RecyclerView
    private lateinit var adapter: TreeAdapter
    private lateinit var titleTv : TextView
    private lateinit var removeFarmButton : MaterialButton
    private lateinit var selectedTree : Tree

    var farm : Farm? = null
    var trees: MutableList<Tree> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_farm)

        rcTrees = findViewById<RecyclerView>(R.id.rcTrees)
        titleTv = findViewById<TextView>(R.id.titleTv)
        removeFarmButton = findViewById<MaterialButton>(R.id.removeFarmButton)

        getIntentData()
        setUpTrees()

        titleTv.text = farm?.name

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private val detailResultLauncher = this.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val plantDetail = data?.getSerializableExtra("plantDetail") as? PlantResponse

            selectedTree.data = gson.toJson(plantDetail)

            val sharedPref = this@FarmActivity.getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
            val type = object : TypeToken<MutableList<Farm>>() {}.type
            val farmList: MutableList<Farm> =
                gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

            val farm = farmList.find { it.id == farm?.id }
            val tree = farm?.trees?.find { it.id == selectedTree.id}
            tree?.data = gson.toJson(plantDetail)

            sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()
            trees = farm?.trees?.toMutableList() ?: mutableListOf()
            setUpTrees()

        }
    }

    private fun getIntentData() {
        //kung deprecated oks lang yan haha
        farm = intent.getSerializableExtra("farm") as? Farm
        trees = farm?.trees?.toMutableList() ?: mutableListOf()
        if (farm != null) {
            // Use the data
            Log.d("FarmActivity", "Name: ${farm?.name}")
        }
    }

    private fun setUpTrees() {
        rcTrees.layoutManager = GridLayoutManager(this, 1)
        adapter = TreeAdapter(
            trees,
            onCheckDetailClick = { tree ->

                if(tree.data == null)
                {
                    Toast.makeText(this, "Diagnose first to add history.", Toast.LENGTH_SHORT).show()
                }else{
                    val intent = Intent(this, TreeHistoryActivity::class.java)
                    intent.putExtra("tree" , tree)
                    detailResultLauncher.launch(intent)
                }


            },
            onDiagnoseClick = { tree ->
                selectedTree = tree
                val intent = Intent(this, PlantScanActivity::class.java)
                detailResultLauncher.launch(intent)
            }
        )

        rcTrees.adapter = adapter
    }

    fun AddTree(view: View) {
        val sharedPref = this@FarmActivity.getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)

        val type = object : TypeToken<MutableList<Farm>>() {}.type
        val farmList: MutableList<Farm> =
            gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
            Date()
        )

        val farm = farmList.find { it.id == farm?.id }
        val sortedLastTree = farm?.trees?.sortedBy { it.id }?.lastOrNull()
        val id : Int = if(sortedLastTree == null) {
            1
        } else {
            sortedLastTree.id + 1
        }

        val newTree = Tree(
            name = "Tree $id",
            id = id,
            plantedDate = currentDate,
            status = "",
            iconRes = 0,
            data = null,
            history = mutableListOf())

        val updatedFarm = farm?.copy(trees = (farm.trees + newTree).toMutableList())

        if (updatedFarm != null) {
            val index = farmList.indexOfFirst { it.id == updatedFarm.id }
            if (index != -1) {
                farmList[index] = updatedFarm
            }
        }

        sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()
        adapter.addTree(newTree)
        trees = updatedFarm?.trees?.toMutableList() ?: mutableListOf()

        setUpTrees()
    }

    fun RemoveFarm(view: View) {
        val sharedPref = this@FarmActivity.getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val gson = Gson()

        val type = object : TypeToken<MutableList<Farm>>() {}.type
        val farmList: MutableList<Farm> =
            gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

        farmList.remove(farm)
        sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()


        val intent = Intent()
        intent.putExtra("key", "value")
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onBackPressed() {
        val intent = Intent()
        intent.putExtra("key", "value")
        setResult(Activity.RESULT_OK, intent)
        finish()

        super.onBackPressed()
    }
}