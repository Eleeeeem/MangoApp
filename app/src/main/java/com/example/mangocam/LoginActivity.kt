package com.example.mangocam

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mangocam.utils.Constant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvSignUp = findViewById(R.id.tvSignUp)

        btnLogin.setOnClickListener { loginUser() }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordChoiceActivity::class.java)
            // Pass along the email field if user already typed it (optional UX sugar)
            intent.putExtra("prefill_email", etEmail.text.toString().trim())
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setMessage("Logging in...")
        progressDialog.show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener

                firestore.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener { doc ->
                        progressDialog.dismiss()
                        if (doc.exists()) {
                            val sharedPref = getSharedPreferences(Constant.SHARED_PREF_USER, Context.MODE_PRIVATE)
                            sharedPref.edit()
                                .putString("userId", userId)
                                .putString("name", doc.getString("name"))
                                .putString("email", doc.getString("email"))
                                .putString("address", doc.getString("address"))
                                .putString("contact", doc.getString("contact")) // store phone here
                                .putString("birthday", doc.getString("birthday"))
                                .putString("dateJoined", doc.getString("dateJoined"))
                                .apply()

                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "User record not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }
    }
}
