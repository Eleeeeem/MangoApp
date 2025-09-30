package com.example.mangocam.utils

import android.graphics.Color
import com.example.mangoo.DiseaseHistory
import com.example.mangoo.DiseaseSuggestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

object PlantDescriptionCreator {
    private val specificDiseaseMap = mapOf(
        "Fungi" to mapOf(
            "powdery" to "Powdery Mildew",
            "downy" to "Downy Mildew",
            "blight" to "Leaf Blight",
            "rust" to "Rust Fungus",
            "spot" to "Leaf Spot",
            "mildew" to "Mildew Infection"
        ),
        "Insecta" to mapOf(
            "aphid" to "Aphid Infestation",
            "mite" to "Spider Mites",
            "thrip" to "Thrips Damage",
            "scale" to "Scale Insects",
            "mealybug" to "Mealybug Infestation"
        )
    )

    private val treatmentMap = mapOf(
        "Fungi" to "🍄 Treatment:\n• Apply fungicide.\n• Improve air circulation.\n• Avoid overhead watering.",
        "Bacteria" to "🦠 Treatment:\n• Apply copper-based bactericide.\n• Remove and destroy infected leaves.\n• Avoid overhead watering.",
        "Animalia" to "🐛 Treatment:\n• Use insecticidal soap.\n• Remove affected parts.",
        "Insecta" to "🦗 Treatment:\n• Neem oil spray.\n• Remove infested parts.",
        "mechanical damage" to "🪓 Treatment:\n• Prune damaged areas.\n• Avoid rough handling.",
        "senescence" to "🍂 Treatment:\n• No treatment needed – natural aging.",
        "nutrient deficiency" to "🌿 Treatment:\n• Apply fertilizer.\n• Check pH and water regularly.",
        "light excess" to "🔆 Treatment:\n• Provide partial shade.",
        "water excess" to "💧 Treatment:\n• Improve drainage.\n• Avoid overwatering.",
        "uneven watering" to "🚿 Treatment:\n• Water evenly.\n• Mulch to retain moisture."
    )

    fun getSpecificDiseaseName(category: String?, description: String?, fallbackName: String?): String? {
        val keywordMap = specificDiseaseMap[category ?: return null] ?: return null
        val combinedText = listOfNotNull(description, fallbackName).joinToString(" ")

        for ((keyword, specificName) in keywordMap) {
            if (combinedText.contains(keyword, ignoreCase = true)) {
                return specificName
            }
        }
        return null
    }

    fun showHealthyMessage(plantName: String) : DiseaseHistory {
        return DiseaseHistory(
            plantName = plantName,
            diseaseName = "Healthy",
            accuracy = "100%",
            treatment = "No treatment needed",
            date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )
    }

    fun showDiseaseDetails(diseases: List<DiseaseSuggestion>, plantName: String) : DiseaseHistory {
        val topDisease = diseases.maxByOrNull { it.probability ?: 0.0 }
        val genericName = topDisease?.name ?: "Unknown"
        val specificName =
            getSpecificDiseaseName(genericName, topDisease?.description, topDisease?.name)
        val name = specificName ?: genericName
        val accuracy = topDisease?.probability.toPercentageString()
        val treatmentText = treatmentMap[genericName]
            ?: "• No specific treatment found. Consider general plant care."

        val history = DiseaseHistory(
            plantName = plantName,
            diseaseName = name,
            accuracy = accuracy,
            treatment = treatmentText,
            date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )
        return history
    }

    private fun Double?.toPercentageString(): String {
        return String.format(Locale.getDefault(), "%.2f%%", (this ?: 0.0) * 100)
    }
}