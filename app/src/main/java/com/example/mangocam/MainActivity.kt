package com.example.mangocam

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel() // 🔔 Create channel on app start

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val selectedFragment = intent.getStringExtra("selectedFragment")

        if (savedInstanceState == null) {
            when (selectedFragment) {
                "logs" -> {
                    bottomNav.selectedItemId = R.id.nav_settings
                    replaceFragment(LogsFragment())
                }
                "profile" -> {
                    bottomNav.selectedItemId = R.id.nav_profile
                    replaceFragment(ProfileFragment())
                }
                else -> {
                    bottomNav.selectedItemId = R.id.nav_disease
                    replaceFragment(DiseaseFragment())
                }
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_disease -> replaceFragment(DiseaseFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                R.id.nav_settings -> replaceFragment(LogsFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "harvest_channel_id",
                "Harvest Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for harvest day reminders"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
