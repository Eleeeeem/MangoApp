package com.example.mangocam

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.adapter.TreeHistoryAdapter
import com.example.mangocam.model.Tree
import com.example.mangoo.DiseaseSuggestion
import com.example.mangoo.PlantResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TreeHistoryActivity : AppCompatActivity() {

    val gson = Gson()
    private lateinit var rcHistory: RecyclerView
    private lateinit var adapter: TreeHistoryAdapter
    private lateinit var healthyTagTv : TextView
    var tree : Tree? = null
    var treeDetails : PlantResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tree_history)

        rcHistory = findViewById<RecyclerView>(R.id.diseaseRc)
        healthyTagTv = findViewById<TextView>(R.id.healthyTagTv)

        getIntentData()
        checkDetailAndSetupHistory()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun getIntentData() {
        //kung deprecated oks lang yan haha
        tree = intent.getSerializableExtra("tree") as? Tree

        if(tree != null)
        {
            val type = object : TypeToken<PlantResponse>() {}.type
            treeDetails = gson.fromJson(tree?.data, type)
        }

    }

    private fun checkDetailAndSetupHistory() {
        if(treeDetails?.result?.disease?.suggestions == null)
        {
            rcHistory.visibility = View.GONE
            healthyTagTv.visibility = View.VISIBLE
            return
        }

        rcHistory.visibility = View.VISIBLE
        healthyTagTv.visibility = View.GONE
        setUpdateDiseaseHistory()
    }

    private fun setUpdateDiseaseHistory() {
        rcHistory.layoutManager = GridLayoutManager(this, 1)

        val highestProbabilityDiseases: List<DiseaseSuggestion> = treeDetails
            ?.result
            ?.disease
            ?.suggestions
            ?.let { list ->
                val maxProbability = list.maxOfOrNull { it.probability ?: 0.0 }
                list.filter { (it.probability ?: 0.0) == maxProbability }
            } ?: emptyList()

        adapter = TreeHistoryAdapter(
            highestProbabilityDiseases,
            treeDetails?.date.toString()
        )

        rcHistory.adapter = adapter
    }

    private val detailResultLauncher = this.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

        }
    }

}