package com.example.mangocam

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangocam.adapter.FarmAdapter
import com.example.mangocam.model.Farm
import com.example.mangocam.utils.Constant
import com.example.mangocam.utils.SharedPrefUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.prolificinteractive.materialcalendarview.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.coroutines.resume

class LogsFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var tvHarvestDate: TextView
    private lateinit var tvNextSprayDate: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FarmAdapter
    private lateinit var addTreeButton: MaterialButton

    private lateinit var noSprayTv: TextView
    private lateinit var farmNameTv: TextView
    private lateinit var addSprayButton: MaterialButton
    private lateinit var logView: LinearLayout

    private var selectedCalendarDay: CalendarDay? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.logs_fragment, container, false)

        calendarView = view.findViewById(R.id.calendarView)
        tvHarvestDate = view.findViewById(R.id.tvHarvestDate)
        tvNextSprayDate = view.findViewById(R.id.tvNextSprayDate)
        recyclerView = view.findViewById(R.id.recyclerViewTrees)
        addTreeButton = view.findViewById(R.id.addTreeButton)
        addSprayButton = view.findViewById(R.id.addSprayButton)
        noSprayTv = view.findViewById(R.id.noSprayTv)
        logView = view.findViewById(R.id.logView)
        farmNameTv = view.findViewById(R.id.farmNameTv)

        calendarView.setOnDateChangedListener { _, date, _ ->
            updateDetailView(date)
        }

        addTreeButton.setOnClickListener { addFarm() }
        addSprayButton.setOnClickListener { selectFarmForSpray() }

        addCalendarMarks()
        setupFarms()
        checkHarvestNotifications()

        return view
    }

    // Show farms for the selected date
    private fun updateDetailView(selectedCalendarDate: CalendarDay) {
        selectedCalendarDay = selectedCalendarDate

        val sharedPref = requireContext().getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val farms = SharedPrefUtil.getFarms(sharedPref)

        val selectedDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.of(selectedCalendarDate.year, selectedCalendarDate.month + 1, selectedCalendarDate.day)
        } else null

        // ✅ Find all farms that share the same spray date
        val matchingFarms = farms.filter { farm ->
            farm.sprayDate?.let {
                try {
                    selectedDate != null && LocalDate.parse(it) == selectedDate
                } catch (_: Exception) {
                    false
                }
            } ?: false
        }

        // If no farms match this date
        if (matchingFarms.isEmpty()) {
            noSprayTv.visibility = View.VISIBLE
            addSprayButton.visibility = View.VISIBLE
            logView.visibility = View.GONE
            addCalendarMarks()
            return
        }

        // ✅ Show all farm names separated by commas
        noSprayTv.visibility = View.GONE
        addSprayButton.visibility = View.GONE
        logView.visibility = View.VISIBLE

        val farmNamesList = matchingFarms.joinToString(", ") { it.name }
        farmNameTv.text = farmNamesList

        // ✅ Use first one just for the highlight computation
        matchingFarms.firstOrNull()?.let {
            highlightSprayDaysOnwards(selectedCalendarDate, it)
        }
    }



    // 🌾 Selecting farms for spray (only farms WITH trees)
    private fun selectFarmForSpray() {
        val date = selectedCalendarDay ?: run {
            Toast.makeText(requireContext(), "Select a date first", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = requireContext().getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val farmList = SharedPrefUtil.getFarms(sharedPref)

        // ✅ Only farms that have trees and no existing spray date
        val eligibleFarms = farmList.filter { it.sprayDate == null && it.trees.isNotEmpty() }

        if (eligibleFarms.isEmpty()) {
            Toast.makeText(requireContext(), "No farms with trees available for spraying", Toast.LENGTH_SHORT).show()
            return
        }

        val farmNames: Array<CharSequence> = eligibleFarms.map { it.name }.toTypedArray()
        val checkedItems = BooleanArray(eligibleFarms.size)

        MaterialAlertDialogBuilder(requireContext(), R.style.MangoDialogStyle)
            .setTitle("Select Farms to Spray")
            .setMultiChoiceItems(farmNames, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("OK") { dialog, _ ->
                val selectedFarmNames = eligibleFarms
                    .filterIndexed { index, _ -> checkedItems[index] }
                    .map { it.name }

                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance().apply {
                    set(date.year, date.month, date.day)
                }
                val formattedDate = formatter.format(calendar.time)

                for (farm in farmList) {
                    if (farm.name in selectedFarmNames) {
                        farm.sprayDate = formattedDate
                    }
                }

                SharedPrefUtil.setFarms(sharedPref, farmList)
                updateDetailView(date)
                setupFarms()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()

    }



    // Add decorators for the calendar using saved spray dates
    private fun addCalendarMarks() {
        calendarView.removeDecorators()
        val sharedPref = requireContext().getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val farmList = SharedPrefUtil.getFarms(sharedPref)

        val farmWithSprayDates: List<Farm> = farmList.filter { it.sprayDate != null }

        calendarView.post {
            calendarView.addDecorator(TodayDecorator())

            val sprayDates = farmWithSprayDates.mapNotNull { farm ->
                farm.sprayDate?.let { stringToCalendarDay(it) }
            }.toSet()

            calendarView.addDecorator(SprayDecorator(sprayDates))
            calendarView.invalidateDecorators()
        }
    }

    private val activityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshList()
        }
    }

    private fun refreshList() {
        setupFarms()
    }

    private fun setupFarms() {
        val sharedPref = requireContext().getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<Farm>>() {}.type
        val farmList: MutableList<Farm> =
            gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        adapter = FarmAdapter(
            farmList,
            onClick = { farm ->
                // when farm is clicked -> open details
                val intent = Intent(requireActivity(), FarmActivity::class.java)
                intent.putExtra("farm", farm)
                activityLauncher.launch(intent)
            },
            onDelete = { farmToDelete ->
                // Only delete, NO dialog here
                farmList.remove(farmToDelete)
                sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()
                adapter.notifyDataSetChanged()
            }
        )
        recyclerView.adapter = adapter
    }


    // Highlight spray schedule ranges (30/90/110 day windows)
    private fun highlightSprayDaysOnwards(startDate: CalendarDay, matchingFarm: Farm) {
        val creamDates = mutableListOf<CalendarDay>()
        val calendar = Calendar.getInstance().apply { set(startDate.year, startDate.month, startDate.day) }

        repeat(110) {
            creamDates.add(CalendarDay.from(calendar))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val harvestDate = creamDates.last().date
        val formattedHarvest = android.text.format.DateFormat.format("MMMM dd, yyyy", harvestDate)

        // Next spray session (90–96 days)
        val greenDates = mutableListOf<CalendarDay>()
        val greenCalendar = Calendar.getInstance().apply { set(startDate.year, startDate.month, startDate.day); add(Calendar.DAY_OF_MONTH, 90) }
        repeat(7) {
            greenDates.add(CalendarDay.from(greenCalendar)); greenCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        val formattedNextSpray = android.text.format.DateFormat.format("MMMM dd, yyyy", greenDates.first().date)

        // Yellow stage (30–60 days)
        val yellowDates = mutableListOf<CalendarDay>()
        val yellowCalendar = Calendar.getInstance().apply { set(startDate.year, startDate.month, startDate.day); add(Calendar.DAY_OF_MONTH, 30) }
        repeat(30) {
            yellowDates.add(CalendarDay.from(yellowCalendar)); yellowCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val orangeDate = creamDates.last()

        // Reset decorators and re-apply
        calendarView.removeDecorators()
        calendarView.addDecorator(TodayDecorator())
        calendarView.addDecorator(createDecorator(creamDates, R.drawable.highlight_circle))
        calendarView.addDecorator(createDecorator(greenDates, R.drawable.green_circle))
        calendarView.addDecorator(createDecorator(yellowDates, R.drawable.yellow_circle))
        calendarView.addDecorator(createDecorator(listOf(orangeDate), R.drawable.orange_circle))

        // Update labels
        tvHarvestDate.text = formattedHarvest
        tvNextSprayDate.text = formattedNextSpray
//        farmNameTv.text = matchingFarm.name
    }

    private fun createDecorator(dates: List<CalendarDay>, drawableRes: Int): DayViewDecorator {
        return object : DayViewDecorator {
            override fun shouldDecorate(day: CalendarDay) = dates.contains(day)
            override fun decorate(view: DayViewFacade) {
                ContextCompat.getDrawable(requireContext(), drawableRes)?.let { view.setBackgroundDrawable(it) }
            }
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    inner class TodayDecorator : DayViewDecorator {
        private val today = CalendarDay.today()
        override fun shouldDecorate(day: CalendarDay) = day == today
        override fun decorate(view: DayViewFacade) {
            ContextCompat.getDrawable(requireContext(), R.drawable.today_circle)?.let { view.setBackgroundDrawable(it) }
        }
    }

    inner class SprayDecorator(private val dates: Set<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
        override fun decorate(view: DayViewFacade) {
            ContextCompat.getDrawable(requireContext(), R.drawable.yellow_circle)?.let { view.setBackgroundDrawable(it) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.profile_menu, menu)
        val menuItem = menu.findItem(R.id.action_notification)
        val actionView = menuItem.actionView
        val badgeTextView = actionView?.findViewById<TextView>(R.id.badge_text_view)
        val unreadCount = 3
        badgeTextView?.apply { text = unreadCount.toString(); visibility = if (unreadCount > 0) View.VISIBLE else View.GONE }
        actionView?.setOnClickListener { onOptionsItemSelected(menuItem) }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> { logoutUser(); true }
            R.id.action_notification -> { showHarvestEndedNotification(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showHarvestEndedNotification() {
        vibrateDevice()
        val builder = NotificationCompat.Builder(requireContext(), "harvest_channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🥭 Harvest Time!")
            .setContentText("The 110-day spray schedule has ended. Time to harvest your mangoes!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        NotificationManagerCompat.from(requireContext()).notify(1001, builder.build())
    }

    private fun vibrateDevice() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = requireContext().getSystemService(android.os.VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }

        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
    }
    private fun checkHarvestNotifications() {
        val sharedPref = requireContext().getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<Farm>>() {}.type
        val farmList: MutableList<Farm> =
            gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

        val today = Calendar.getInstance().time
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (farm in farmList) {
            farm.sprayDate?.let { sprayDateStr ->
                try {
                    val sprayDate = sdf.parse(sprayDateStr)
                    val cal = Calendar.getInstance().apply { time = sprayDate!! }
                    cal.add(Calendar.DAY_OF_YEAR, 120) // Harvest day

                    if (isSameDay(today, cal.time)) {
                        showHarvestNotification(farm.name)
                        vibrateDevice()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showHarvestNotification(farmName: String) {
        val builder = NotificationCompat.Builder(requireContext(), "harvest_channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🥭 Harvest Time!")
            .setContentText("It’s harvest day for \"$farmName\". Time to pick your mangoes!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(requireContext()).notify(farmName.hashCode(), builder.build())
    }


    // Add farm flow: prompt user and persist
    fun addFarm() {
        lifecycleScope.launch {
            val name = requireContext().showAddFarmDialog()
            if (name != null && name.isNotBlank()) {
                val sharedPref = requireContext().getSharedPreferences(Constant.SHARED_PREF_FARM, Context.MODE_PRIVATE)
                val gson = Gson()
                val type = object : TypeToken<MutableList<Farm>>() {}.type
                val farmList: MutableList<Farm> =
                    gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

                val newFarm = Farm(name = name, id = UUID.randomUUID().toString(), sprayDate = null, trees = mutableListOf())
                farmList.add(newFarm)
                sharedPref.edit().putString(Constant.SHARED_PREF_FARM, gson.toJson(farmList)).apply()
                setupFarms()
                Toast.makeText(requireContext(), "Farm \"$name\" added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Modern suspendable add-farm dialog using Material style
    suspend fun Context.showAddFarmDialog(): String? = suspendCancellableCoroutine { cont ->
        val editText = EditText(this).apply {
            hint = "Enter farm name"
            setHintTextColor(Color.parseColor("#808080"))
            setTextColor(Color.BLACK)
            setPadding(60, 50, 60, 50)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
            addView(editText)
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.MangoDialogStyle)
            .setTitle("🌾 Add New Farm")
            .setMessage("Please enter a name for your new farm.")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = editText.text.toString().trim()
                cont.resume(if (name.isEmpty()) null else name)
            }
            .setNegativeButton("Cancel") { _, _ ->
                cont.resume(null)
            }
            .setOnCancelListener { cont.resume(null) }
            .create()

        dialog.show() // then show

        // ✅ Apply black text colors
        dialog.apply {
            findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(Color.BLACK)
            findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
        }
        cont.invokeOnCancellation { dialog.dismiss() }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // Converts yyyy-MM-dd string to CalendarDay
    fun stringToCalendarDay(dateString: String): CalendarDay? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateString) ?: return null
            val calendar = Calendar.getInstance().apply { time = date }
            CalendarDay.from(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }
}
