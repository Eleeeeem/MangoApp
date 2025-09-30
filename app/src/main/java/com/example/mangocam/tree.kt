package com.example.mangocam.model

import java.io.Serializable

data class Farm(
    var trees: List<Tree>,
    val sprayDate : String?,
    val name : String,
    val id : String
)  : Serializable

data class Tree(
    val name : String ="",
    val id: String = "",
    val plantedDate: String = "",
    val status: String = "",
    val iconRes: Int = 0,
    val history: MutableList<TreeHistory> = mutableListOf(),
    var data : String?
): Serializable

data class TreeHistory(
    val date: String = "",
    val diseaseDetected: String? = null,
    val treatment: String? = null,
    val action: String = "" // e.g. "Sprayed", "Fertilized"
): Serializable
