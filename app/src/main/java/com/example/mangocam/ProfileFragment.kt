package com.example.mangocam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.model.Farm
import com.example.mangocam.utils.Constant
import com.example.mangoo.DiseaseHistory
import com.example.mangoo.HistoryAdapter
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var tvFullname: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvFarmLocation: TextView
    private lateinit var tvCrops: TextView
    private lateinit var tvJoinedDate: TextView
    private lateinit var tvContact: TextView
    private lateinit var btnLogout: Button
    private lateinit var profileImage: ImageView

    private lateinit var dbRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Bind views
        profileImage = view.findViewById(R.id.profileImage)
        tvFullname = view.findViewById(R.id.tvFullname)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvFarmLocation = view.findViewById(R.id.tvFarmLocation)
        tvCrops = view.findViewById(R.id.tvCrops)
        tvJoinedDate = view.findViewById(R.id.tvJoinedDate)
        tvContact = view.findViewById(R.id.tvContact)
        btnLogout = view.findViewById(R.id.btnLogout)

        dbRef = FirebaseDatabase.getInstance().getReference("users")

        // ✅ Get userId from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("userId", null)

        if (userId != null) {
            // 1. Show cached data instantly
            loadUserProfileFromCache()

            // 2. Then refresh from Firebase (background update)
            loadUserProfileFromFirebase(userId)
        } else {
            Toast.makeText(requireContext(), "No user found, please log in again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        btnLogout.setOnClickListener {
            logoutUser()
        }

        // ✅ Load saved history
        loadHistory(view)

        return view
    }

    // 🔹 Load cached profile details (instant)
    private fun loadUserProfileFromCache() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val farmPref = requireActivity().getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)


        val farmJson = farmPref.getString(Constant.SHARED_PREF_FARM, null)
        val farms: List<Farm> = if (farmJson != null) {
            val type = object : TypeToken<List<Farm>>() {}.type
            Gson().fromJson(farmJson, type)
        } else {
            emptyList()
        }

        val totalTreeCount = farms.sumOf { it.trees.size }


        tvFullname.text = sharedPref.getString("name", "Unknown")
        tvEmail.text = sharedPref.getString("email", "No email")
        tvFarmLocation.text = sharedPref.getString("address", "No address")
        tvCrops.text = "Mango Trees: ${totalTreeCount}"
        tvJoinedDate.text = sharedPref.getString("joinedDate", "Joined: 2025") // fallback
        tvContact.text = sharedPref.getString("contact", "No contact")
    }

    // 🔹 Sync with Firebase and update cache
    private fun loadUserProfileFromFirebase(userId: String) {
        dbRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(HelperClass::class.java)
                if (user != null) {

                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd- MMM-yyyy", Locale.getDefault())

                    val dateStr = user.dateJoined

                    if(dateStr == null)
                    {
                        tvJoinedDate.text = "Joined: Unknown"
                    }else
                    {
                        val parsedDate = inputFormat.parse(dateStr)
                        val formattedDate = parsedDate?.let { outputFormat.format(it) } ?: "Unknown"
                        tvJoinedDate.text = "Joined: ${formattedDate}"
                    }

                    tvFullname.text = user.name
                    tvEmail.text = user.email
                    tvFarmLocation.text = user.address

                    tvContact.text = user.contact

                    // update cache
                    val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                    sharedPref.edit()
                        .putString("name", user.name)
                        .putString("email", user.email)
                        .putString("address", user.address)
                        .putString("mangoTrees", user.mangoTrees.toString())
                        .putString("contact", user.contact)
                        .apply()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to refresh profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 🔹 History loader
    private fun loadHistory(rootView: View) {
        val prefs = requireContext().getSharedPreferences("disease_history", Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<List<DiseaseHistory>>() {}.type
        val json = prefs.getString("history", null)

        val historyList: List<DiseaseHistory> = if (json != null) {
            gson.fromJson(json, type)
        } else emptyList()

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

    // 🔹 Logout clears everything
    private fun logoutUser() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        Toast.makeText(requireContext(), "Logged out!", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
