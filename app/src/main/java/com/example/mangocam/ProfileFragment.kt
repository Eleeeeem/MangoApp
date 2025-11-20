package com.example.mangocam

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.model.Tree
import com.example.mangocam.utils.Constant
import com.example.mangoo.DiseaseHistory
import com.example.mangoo.HistoryAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var tvFullname: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvFarmLocation: TextView
    private lateinit var tvCrops: TextView
    private lateinit var tvJoinedDate: TextView
    private lateinit var tvContact: TextView
    private lateinit var btnLogout: View
    private lateinit var profileImage: ImageView

    private lateinit var firestore: FirebaseFirestore
    private var userId: String? = null
    private lateinit var auth: FirebaseAuth
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        firestore = FirebaseFirestore.getInstance()
        val sharedPref = requireContext().getSharedPreferences(Constant.SHARED_PREF_USER, Context.MODE_PRIVATE)
        userId = sharedPref.getString(Constant.SHARED_PREF_USER_DETAIL_USERID, null)

        auth = FirebaseAuth.getInstance()

        profileImage = view.findViewById(R.id.profileImage)
        tvFullname = view.findViewById(R.id.tvFullname)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvFarmLocation = view.findViewById(R.id.tvFarmLocation)
        tvCrops = view.findViewById(R.id.tvCrops)
        tvJoinedDate = view.findViewById(R.id.tvJoinedDate)
        tvContact = view.findViewById(R.id.tvContact)
        btnLogout = view.findViewById(R.id.btnLogout)

        val user = auth.currentUser
        if (user != null) {
            loadUserProfile(user.uid)
        } else {
            logoutUser()
        }

        // ✅ Show confirmation dialog before logging out
        btnLogout.setOnClickListener {
            requireContext().showLogoutDialog {
                logoutUser()
            }
        }
        lifecycleScope.launch {
            loadHistory(view)
            var trees = getAllTreesForUser(userId!!)
            tvCrops.text = "Mango Trees: ${trees.size ?: 0}"
        }

        return view
    }

    suspend fun getAllTreesForUser(userId: String): List<Tree> {

        val allTrees = mutableListOf<Tree>()

        val farmsSnapshot = firestore.collection("users")
            .document(userId)
            .collection("farms")
            .get()
            .await()

        for (farmDoc in farmsSnapshot.documents) {
            val farmId = farmDoc.id

            val treesSnapshot = firestore.collection("users")
                .document(userId)
                .collection("farms")
                .document(farmId)
                .collection("trees")
                .get()
                .await()

            val trees = treesSnapshot.toObjects(Tree::class.java)
            allTrees.addAll(trees)
        }

        return allTrees
    }


    private fun loadUserProfile(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                if (doc.exists()) {
                    tvFullname.text = doc.getString("name") ?: "Unknown"
                    tvEmail.text = doc.getString("email") ?: "N/A"
                    tvFarmLocation.text = doc.getString("address") ?: "Not set"
                    tvContact.text = doc.getString("contact") ?: "N/A"

                    val dateJoinedValue = doc.get("dateJoined")
                    val formattedDate = when (dateJoinedValue) {
                        is com.google.firebase.Timestamp -> {
                            val date = dateJoinedValue.toDate()
                            val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            sdf.format(date)
                        }
                        is String -> dateJoinedValue
                        else -> "Unknown"
                    }
                    tvJoinedDate.text = "Joined: $formattedDate"
                } else {
                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private suspend fun loadHistory(rootView: View) {
        val historyCollection = firestore.collection("users")
            .document(userId!!)
            .collection("history")

        val snapshot = historyCollection.get().await()
        val historyList = snapshot.documents.mapNotNull { doc ->
            doc.toObject(DiseaseHistory::class.java)
        }
        historyList.sortedByDescending { it.date }

        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerHistory)
        val tvNoHistory = rootView.findViewById<TextView>(R.id.tvNoHistory)

        if (historyList.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvNoHistory.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvNoHistory.visibility = View.GONE
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = HistoryAdapter(historyList)
        }
    }

    // ✅ Simplified, consistent with AddFarmDialog
    fun Context.showLogoutDialog(onConfirm: () -> Unit) {
        val dialog = MaterialAlertDialogBuilder(this, R.style.MangoDialogStyle)
            .setTitle("🚪 Logout")
            .setMessage("Are you sure you want to log out of your account?")
            .setPositiveButton("Logout") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(Color.BLACK)
        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)

        // ✅ Force button text to black
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

    private fun logoutUser() {
        auth.signOut()
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        uiScope.cancel()
    }
}
