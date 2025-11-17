package com.example.mangocam

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etBirthday: EditText
    private lateinit var etContact: EditText
    private lateinit var btnVerify: Button

    private lateinit var layoutNewPassword: TextInputLayout
    private lateinit var layoutConfirmPassword: TextInputLayout
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnResetPassword: Button

    private val firestore = FirebaseFirestore.getInstance()
    private var verifiedUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialize views
        etEmail = findViewById(R.id.etEmailReset)
        etBirthday = findViewById(R.id.etBirthdayReset)
        etContact = findViewById(R.id.etContactReset)
        btnVerify = findViewById(R.id.btnVerify)

        layoutNewPassword = findViewById(R.id.layoutNewPassword)
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)

        // Ensure password layouts are initially hidden
        layoutNewPassword.visibility = TextInputLayout.GONE
        layoutConfirmPassword.visibility = TextInputLayout.GONE
        btnResetPassword.visibility = Button.GONE

        // Pre-fill contact if passed from intent
        val passedNumber = intent.getStringExtra("prefill_number")
        if (!passedNumber.isNullOrEmpty()) {
            etContact.setText(passedNumber)
            etContact.isEnabled = false
        }

        // Listeners
        etBirthday.setOnClickListener { showDatePicker() }
        btnVerify.setOnClickListener { verifyUser() }
        btnResetPassword.setOnClickListener { resetPassword() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, y, m, d -> etBirthday.setText("${m + 1}/$d/$y") },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    // Normalize contact number (+63 or 0)
    private fun normalizeContact(contact: String): String {
        var formatted = contact.trim()
        if (formatted.startsWith("0")) {
            formatted = formatted.replaceFirst("0", "+63")
        }
        return formatted
    }

    private fun verifyUser() {
        val email = etEmail.text.toString().trim()
        val birthday = etBirthday.text.toString().trim()
        val contact = etContact.text.toString().trim()

        if (email.isEmpty() || birthday.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedContact = normalizeContact(contact)
        Log.d("VERIFY", "email=$email, birthday=$birthday, contact=$normalizedContact")

        firestore.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("birthday", birthday)
            .whereEqualTo("contact", normalizedContact)
            .get()
            .addOnSuccessListener { result ->
                Log.d("VERIFY", "Documents found: ${result.size()}")

                if (!result.isEmpty) {
                    val document = result.documents[0]
                    verifiedUserId = document.id

                    Toast.makeText(
                        this,
                        "Account verified! You can now reset your password.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Show password fields
                    layoutNewPassword.visibility = TextInputLayout.VISIBLE
                    layoutConfirmPassword.visibility = TextInputLayout.VISIBLE
                    btnResetPassword.visibility = Button.VISIBLE

                    btnVerify.isEnabled = false
                } else {
                    Toast.makeText(this, "No matching account found", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error verifying user: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("VERIFY", "Error verifying user", e)
            }
    }

    private fun resetPassword() {
        val newPass = etNewPassword.text.toString().trim()
        val confirmPass = etConfirmPassword.text.toString().trim()

        // Validate empty fields
        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please enter both password fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Password mismatch check
        if (newPass != confirmPass) {
            // Show Toast + inline error
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()

            layoutNewPassword.error = "Passwords do not match"
            layoutConfirmPassword.error = "Passwords do not match"

            // Optionally clear confirm field
            etConfirmPassword.text?.clear()

            return
        } else {
            // Clear errors if passwords match
            layoutNewPassword.error = null
            layoutConfirmPassword.error = null
        }

        // Proceed only if verified
        verifiedUserId?.let { userId ->
            firestore.collection("users")
                .document(userId)
                .update("password", newPass)
                .addOnSuccessListener {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating password: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } ?: run {
            Toast.makeText(this, "Please verify your account first", Toast.LENGTH_SHORT).show()
        }
    }

}
