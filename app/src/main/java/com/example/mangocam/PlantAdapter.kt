package com.example.mangocam

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.example.mangocam.R

class PlantAdapter(
    private val context: Context,
    private val plantList: List<IdentifiedPlant> // ✅ Match the correct data class
) : BaseAdapter() {

    override fun getCount(): Int = plantList.size

    override fun getItem(position: Int): Any = plantList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_plant, parent, false)
        val plant = plantList[position]

        val plantName = view.findViewById<TextView>(R.id.plantName)
        val plantImage = view.findViewById<ImageView>(R.id.plantImage)

        plantName.text = plant.name
        Glide.with(context).load(plant.imageUrl).into(plantImage)

        return view
    }
}
