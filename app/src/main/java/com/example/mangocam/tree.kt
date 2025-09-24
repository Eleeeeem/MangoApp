package com.example.mangocam.model

data class Tree(
    val id: String = "",
    val plantedDate: String = "",
    val status: String = "",
    val iconRes: Int = 0,
    val history: MutableList<TreeHistory> = mutableListOf()
)

data class TreeHistory(
    val date: String = "",
    val diseaseDetected: String? = null,
    val treatment: String? = null,
    val action: String = "" // e.g. "Sprayed", "Fertilized"
)
