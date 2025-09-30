package com.example.mangocam

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mangoo.PlantResponse

interface OnDiseaseDataListener {
    fun onDataReceived(data: PlantResponse?)
}

class PlantScanActivity : AppCompatActivity() , OnDiseaseDataListener {

    lateinit var plantData : PlantResponse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_plant_scan)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, DiseaseFragment())
            .commit()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun SaveTree(view: View) {
        val intent = Intent()
        intent.putExtra("plantDetail", plantData)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onDataReceived(data: PlantResponse?) {
        if (data != null) {
            plantData = data
        }
    }
}