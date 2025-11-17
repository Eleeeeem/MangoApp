package com.example.mangocam

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SetNewPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSetPassword: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_new_password)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSetPassword = findViewById(R.id.btnSetPassword)

        btnSetPassword.setOnClickListener { updatePassword() }
    }

    private fun updatePassword() {
        val newPass = etNewPassword.text.toString().trim()
        val confirmPass = etConfirmPassword.text.toString().trim()

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirmPass) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val email = intent.getStringExtra("user_email")
        val userId = intent.getStringExtra("user_id")

        if (email.isNullOrEmpty() || userId.isNullOrEmpty()) {
            Toast.makeText(this, "Missing account details", Toast.LENGTH_SHORT).show()
            return
        }

        // First, sign in temporarily (if user is not logged in)
        auth.signInWithEmailAndPassword(email, newPass)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    user.updatePassword(newPass)
                        .addOnSuccessListener {
                            firestore.collection("users").document(userId)
                                .update("password", newPass)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Firestore update failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Firebase Auth update failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Temporary sign-in failed. Try again.", Toast.LENGTH_SHORT).show()
            }
    }
}
