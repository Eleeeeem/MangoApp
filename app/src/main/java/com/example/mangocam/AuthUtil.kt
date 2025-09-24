package com.example.mangocam

import android.content.Context
import android.content.Intent
import com.example.mangocam.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

object AuthUtils {
    fun logout(context: Context) {
        Log.d("LogoutDebug", "Logout called")
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}

