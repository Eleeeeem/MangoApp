package com.example.mangocam.model

import java.io.Serializable

data class Farm(
    var trees: MutableList<Tree> = mutableListOf(),
    var sprayDate: String? = null,
    var name: String = "",
    val id: String = ""
) : Serializable

data class Tree(
    var name: String = "",
    val id: Int = 0,
    var firstSprayDate: String? = null,
    val plantedDate: String = "",
    var status: String = "",
    val iconRes: Int = 0,
    var data: String? = null,
    val history: MutableList<TreeHistory> = mutableListOf()
) : Serializable

data class TreeHistory(
    val date: String = "",
    val diseaseDetected: String? = null,
    val treatment: String? = null,
    val action: String = "" // e.g. "Sprayed", "Fertilized"
) : Serializable
