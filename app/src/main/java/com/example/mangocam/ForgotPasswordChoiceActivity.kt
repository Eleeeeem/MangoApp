package com.example.mangocam

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ForgotPasswordChoiceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var etEmail: EditText
    private lateinit var etNumber: EditText
    private lateinit var btnEmailReset: Button
    private lateinit var btnVerifyNumber: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password_choice)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etEmailForReset)
        etNumber = findViewById(R.id.etPhoneForReset)
        btnEmailReset = findViewById(R.id.btnResetByEmail)
        btnVerifyNumber = findViewById(R.id.btnResetByDetails)

        // --- Reset via Email ---
        btnEmailReset.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to send reset link: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // --- Verify Number & Go to ForgotPasswordActivity ---
        btnVerifyNumber.setOnClickListener {
            val number = etNumber.text.toString().trim()
            if (number.isEmpty()) {
                Toast.makeText(this, "Enter your number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firestore.collection("users")
                .whereEqualTo("contact", number)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(this, "Number not found", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Number verified", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ForgotPasswordActivity::class.java)
                        intent.putExtra("prefill_number", number)
                        startActivity(intent)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to verify number: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
