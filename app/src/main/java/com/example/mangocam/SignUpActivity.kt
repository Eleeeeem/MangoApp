package com.example.mangocam

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etContact: EditText
    private lateinit var etAddress: EditText
    private lateinit var etBirthday: EditText
    private lateinit var btnSignUp: Button
    private lateinit var tvLogin: TextView
    private lateinit var passwordError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        progressDialog = ProgressDialog(this)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etContact = findViewById(R.id.etContact)
        etAddress = findViewById(R.id.etAddress)
        etBirthday = findViewById(R.id.etBirthday)
        passwordError = findViewById(R.id.passwordError)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvLogin = findViewById(R.id.tvLogin)

        // Limit PH mobile number
        etContact.filters = arrayOf(InputFilter.LengthFilter(11))
        etContact.inputType = InputType.TYPE_CLASS_PHONE

        // Birthday picker
        etBirthday.inputType = InputType.TYPE_NULL
        etBirthday.setOnClickListener { showDatePicker() }

        // Go to Login
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Show password mismatch in real-time
        etConfirmPassword.addTextChangedListener {
            val pass = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()
            passwordError.visibility = if (confirm.isNotEmpty() && pass != confirm) TextView.VISIBLE else TextView.GONE
        }

        btnSignUp.setOnClickListener { registerUser() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(this, { _, year, month, day ->
            etBirthday.setText("${month + 1}/$day/$year")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePicker.show()
    }

    private fun normalizePhone(phone: String): String? {
        return when {
            phone.matches(Regex("^09\\d{9}$")) -> "+63${phone.substring(1)}"
            phone.matches(Regex("^\\+63\\d{10}$")) -> phone
            else -> null
        }
    }

    private fun registerUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val contact = etContact.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val birthday = etBirthday.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() ||
            confirmPassword.isEmpty() || contact.isEmpty() || address.isEmpty() || birthday.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedPhone = normalizePhone(contact)
        if (formattedPhone == null) {
            Toast.makeText(this, "Enter a valid PH number starting with 09", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setMessage("Creating account...")
        progressDialog.show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener
                val userMap = hashMapOf(
                    "userId" to userId,
                    "name" to name,
                    "email" to email,
                    "password" to password,
                    "contact" to formattedPhone,
                    "address" to address,
                    "birthday" to birthday,
                    "dateJoined" to SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date()),
                    "mangoTrees" to 0
                )
                firestore.collection("users").document(userId)
                    .set(userMap)
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Registration failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
