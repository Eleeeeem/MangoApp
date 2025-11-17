package com.example.mangocam

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class SmsResetActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvPhone: TextView
    private lateinit var btnSendOtp: Button
    private lateinit var etOtp: EditText
    private lateinit var btnVerifyOtp: Button

    private lateinit var newPassLayout: LinearLayout
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSetNewPassword: Button

    private var verificationId: String? = null
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_reset)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        tvPhone = findViewById(R.id.tvPhoneShown)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        etOtp = findViewById(R.id.etOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)

        newPassLayout = findViewById(R.id.layoutNewPassword)
        etNewPassword = findViewById(R.id.etNewPasswordSms)
        etConfirmPassword = findViewById(R.id.etConfirmPasswordSms)
        btnSetNewPassword = findViewById(R.id.btnSetNewPassword)

        phoneNumber = intent.getStringExtra("phone_number") ?: ""
        tvPhone.text = phoneNumber

        btnSendOtp.setOnClickListener { startPhoneVerification(phoneNumber) }
        btnVerifyOtp.setOnClickListener { verifyCode() }
        btnSetNewPassword.setOnClickListener { setNewPassword() }
    }

    private fun startPhoneVerification(phone: String) {
        var formattedPhone = phone
        if (phone.matches(Regex("^09\\d{9}$"))) {
            formattedPhone = "+63" + phone.substring(1)
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(this@SmsResetActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
            verificationId = vid
            forceResendingToken = token
            Toast.makeText(this@SmsResetActivity, "OTP sent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyCode() {
        val code = etOtp.text.toString().trim()
        val vid = verificationId
        if (vid.isNullOrEmpty() || code.isEmpty()) {
            Toast.makeText(this, "Enter the OTP", Toast.LENGTH_SHORT).show()
            return
        }
        val credential = PhoneAuthProvider.getCredential(vid, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        // This signs in the user whose account is linked with this phone number.
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                // SAFETY CHECK: ensure this phone belongs to an existing user who uses email/password (your policy).
                // Optional: check Firestore mapping "users/{uid}.contact == phoneNumber"
                if (user?.phoneNumber == phoneNumber) {
                    // Show new password UI
                    findViewById<View>(R.id.groupOtp).visibility = View.GONE
                    newPassLayout.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Phone not linked to your account. Contact support.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setNewPassword() {
        val p1 = etNewPassword.text.toString().trim()
        val p2 = etConfirmPassword.text.toString().trim()
        if (p1.isEmpty() || p2.isEmpty()) {
            Toast.makeText(this, "Enter both fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (p1 != p2) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in. Please try again.", Toast.LENGTH_LONG).show()
            return
        }
        // Now we can update the password of the SAME Firebase user that is signed-in via phone.
        currentUser.updatePassword(p1)
            .addOnSuccessListener {
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update password: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
