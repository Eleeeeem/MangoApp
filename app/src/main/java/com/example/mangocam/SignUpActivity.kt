package com.example.mangocam

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etContact: EditText
    private lateinit var etAddress: EditText
    private lateinit var etBirthday: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var tvLoginRedirect: TextView
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Views
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etContact = findViewById(R.id.etContact)
        etAddress = findViewById(R.id.etAddress)
        etBirthday = findViewById(R.id.etBirthday)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvLoginRedirect = findViewById(R.id.tvLogin)

        // Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("users")

        // 📅 Show DatePicker when clicking birthday field
        etBirthday.setOnClickListener {
            showDatePicker()
        }

        // Sign Up Button
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val contact = etContact.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val birthday = etBirthday.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Validation
            if (name.isEmpty() || email.isEmpty() || contact.isEmpty() || address.isEmpty() ||
                birthday.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
            ) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generate unique user ID
            val userId = databaseReference.push().key ?: return@setOnClickListener

            // Create user object (set mangoTrees = 0 by default)
            val user = HelperClass(
                userId,
                name,
                email,
                contact,
                address,
                0,          // 👈 Default trees = 0
                birthday,
                password
            )

            // Save to Firebase
            databaseReference.child(userId).setValue(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "Sign Up Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Sign Up Failed!", Toast.LENGTH_SHORT).show()
                }
        }

        // Redirect to login
        tvLoginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                etBirthday.setText(date) // 📌 Saves as "DD/MM/YYYY"
            },
            year, month, day
        )
        datePicker.show()
    }
}
