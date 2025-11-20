package com.example.mangocam

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.adapter.HistoryEntry
import com.example.mangocam.adapter.TreeHistoryAdapter
import com.example.mangocam.model.Tree
import com.example.mangoo.DiseaseSuggestion
import com.example.mangoo.Details
import com.example.mangoo.PlantResponse
import com.example.mangoo.Treatment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class TreeHistoryActivity : AppCompatActivity() {

    private val gson = Gson()
    private lateinit var rcHistory: RecyclerView
    private lateinit var adapter: TreeHistoryAdapter
    private lateinit var healthyTagTv: TextView
    private var tree: Tree? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tree_history)

        rcHistory = findViewById(R.id.diseaseRc)
        healthyTagTv = findViewById(R.id.healthyTagTv)

        tree = intent.getSerializableExtra("tree") as? Tree
        loadAndDisplayHistory(tree)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadAndDisplayHistory(tree : Tree?) {
        if (tree == null) {
            rcHistory.visibility = View.GONE
            healthyTagTv.visibility = View.VISIBLE
            healthyTagTv.text = "No scan history found 🌿"
            return
        }

        val displayItems = mutableListOf<HistoryEntry>()
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        for (treeDetails in tree.data!!) {
            try {
                val suggestions = treeDetails.result?.disease?.suggestions

                val dateStr = treeDetails.date ?: continue
                val parsedDate = sdf.parse(dateStr) ?: continue
                val cal = Calendar.getInstance().apply { time = parsedDate }

                val label = when {
                    isSameDay(cal, today) -> "Today"
                    isSameDay(cal, yesterday) -> "Yesterday"
                    else -> "Earlier"
                }

                val stableImageUri = treeDetails.imageUri

                if (suggestions.isNullOrEmpty()) {
                    // No disease detected → Healthy
                    val healthySuggestion = DiseaseSuggestion(
                        name = "Healthy",
                        probability = 1.0,
                        treatment = null,
                        description = "No disease detected",
                        cause = null,
                        url = null,
                        common_names = null,
                        similar_images = null,
                        imageUri = stableImageUri,
                        details = Details(
                            local_name = "Mango",
                            description = "Healthy plant",
                            url = null,
                            treatment = Treatment(emptyList(), emptyList(), emptyList()),
                            classification = emptyList(),
                            common_names = emptyList(),
                            cause = null,
                            language = "en",
                            entity_id = UUID.randomUUID().toString()
                        )
                    )
                    displayItems.add(HistoryEntry(label, healthySuggestion, dateStr, stableImageUri))
                } else {
                    // Get the disease with the highest probability
                    val maxSuggestion = suggestions.maxByOrNull { it.probability ?: 0.0 } ?: continue
                    displayItems.add(HistoryEntry(label, maxSuggestion, dateStr, stableImageUri))
                }

            } catch (e: Exception) {
                Log.e("TREE_HISTORY_DEBUG", "Error parsing tree data: ${e.message}")
            }
        }

        if (displayItems.isEmpty()) {
            rcHistory.visibility = View.GONE
            healthyTagTv.visibility = View.VISIBLE
            healthyTagTv.text = "Mango is Healthy 🍃"
        } else {
            rcHistory.visibility = View.VISIBLE
            healthyTagTv.visibility = View.GONE
            val sorted = displayItems.sortedByDescending { it.date }

            rcHistory.layoutManager = LinearLayoutManager(this)
            adapter = TreeHistoryAdapter.withGroups(sorted)
            rcHistory.adapter = adapter
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
