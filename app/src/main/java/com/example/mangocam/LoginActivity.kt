package com.example.mangocam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUpRedirect: TextView
    private lateinit var tvForgotPassword: TextView // Add this line
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Views
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUpRedirect = findViewById(R.id.tvSignUp)
        tvForgotPassword = findViewById(R.id.tvForgotPassword) // Initialize here

        // Firebase Database Reference
        databaseReference = FirebaseDatabase.getInstance().getReference("users")

        // Login Button Click (IMPROVED)
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Secure and efficient login check
            databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(HelperClass::class.java)
                            if (user?.password == password) {
                                Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                                getSharedPreferences("UserSession", MODE_PRIVATE)
                                    .edit()
                                    .putString("userId", userSnapshot.key) // Use the unique key
                                    .apply()
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                                return
                            }
                        }
                    }
                    Toast.makeText(this@LoginActivity, "Invalid credentials!", Toast.LENGTH_SHORT).show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@LoginActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Redirect to Sign-Up
        tvSignUpRedirect.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        // Redirect to Forgot Password Activity
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}