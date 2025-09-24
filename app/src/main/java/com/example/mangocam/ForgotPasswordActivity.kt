package com.example.mangocam

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.util.*

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etBirthday: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnVerify: Button
    private lateinit var btnResetPassword: Button

    private lateinit var dbRef: DatabaseReference
    private var attempts = 0
    private val maxAttempts = 3
    private var verifiedUserRef: DatabaseReference? = null // store reference of verified user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialize views
        etUsername = findViewById(R.id.etUsernameReset)
        etBirthday = findViewById(R.id.etBirthdayReset)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnVerify = findViewById(R.id.btnVerify)
        btnResetPassword = findViewById(R.id.btnResetPassword)

        dbRef = FirebaseDatabase.getInstance().getReference("users")

        // Show calendar when clicking birthday
        etBirthday.setOnClickListener {
            showDatePicker()
        }

        // Verify user
        btnVerify.setOnClickListener {
            verifyUser()
        }

        // Reset password
        btnResetPassword.setOnClickListener {
            resetPassword()
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
                etBirthday.setText(date)
            },
            year, month, day
        )
        datePicker.show()
    }

    private fun verifyUser() {
        val username = etUsername.text.toString().trim()
        val birthday = etBirthday.text.toString().trim()

        if (username.isEmpty() || birthday.isEmpty()) {
            Toast.makeText(this, "Enter username and birthday", Toast.LENGTH_SHORT).show()
            return
        }

        dbRef.orderByChild("name").equalTo(username) // 🔹 use 'name' instead of username
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        var matched = false
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(HelperClass::class.java)
                            if (user?.birthday == birthday) {
                                matched = true
                                verifiedUserRef = userSnapshot.ref

                                Toast.makeText(this@ForgotPasswordActivity, "Verification successful!", Toast.LENGTH_SHORT).show()

                                // Show password reset fields
                                etNewPassword.visibility = View.VISIBLE
                                etConfirmPassword.visibility = View.VISIBLE
                                btnResetPassword.visibility = View.VISIBLE

                                btnVerify.isEnabled = false
                                break
                            }
                        }
                        if (!matched) {
                            handleFailedAttempt()
                        }
                    } else {
                        handleFailedAttempt()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ForgotPasswordActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleFailedAttempt() {
        attempts++
        if (attempts >= maxAttempts) {
            Toast.makeText(this, "Too many failed attempts. Try again later.", Toast.LENGTH_LONG).show()
            btnVerify.isEnabled = false
        } else {
            Toast.makeText(this, "Invalid username or birthday. Attempts left: ${maxAttempts - attempts}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetPassword() {
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Enter both password fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        verifiedUserRef?.child("password")?.setValue(newPassword)
            ?.addOnSuccessListener {
                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            ?.addOnFailureListener {
                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
            }
    }
}
