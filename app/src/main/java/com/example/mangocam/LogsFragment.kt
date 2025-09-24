package com.example.mangocam



import android.Manifest

import android.content.Intent

import android.content.pm.PackageManager

import android.os.Build

import android.os.Bundle

import android.view.*

import android.widget.TextView

import android.widget.Toast

import androidx.core.app.ActivityCompat

import androidx.core.content.ContextCompat

import androidx.core.app.NotificationCompat

import androidx.core.app.NotificationManagerCompat

import androidx.fragment.app.Fragment

import androidx.recyclerview.widget.GridLayoutManager

import androidx.recyclerview.widget.RecyclerView

import com.example.mangocam.model.Tree

import com.example.mangocam.ui.logs.TreeAdapter

import com.google.firebase.auth.FirebaseAuth

import com.prolificinteractive.materialcalendarview.*

import java.util.*



class LogsFragment : Fragment() {


    private lateinit var calendarView: MaterialCalendarView

    private lateinit var tvNoLogs: TextView

    private lateinit var tvHarvestDate: TextView

    private lateinit var tvNextSprayDate: TextView

    private lateinit var recyclerView: RecyclerView

    private lateinit var adapter: TreeAdapter

    private var lastSelectedDate: CalendarDay? = null


// Example history map: treeId -> list of spray dates

    private val sprayHistory = mutableMapOf<String, MutableList<String>>()


    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,

        savedInstanceState: Bundle?

    ): View {

        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.logs_fragment, container, false)


// Bind views

        calendarView = view.findViewById(R.id.calendarView)

        tvNoLogs = view.findViewById(R.id.tvNoLogs)

        tvHarvestDate = view.findViewById(R.id.tvHarvestDate)

        tvNextSprayDate = view.findViewById(R.id.tvNextSprayDate)

        recyclerView = view.findViewById(R.id.recyclerViewTrees)


// Setup calendar

        tvNoLogs.text = "Tap a date to mark spray for selected trees"

        calendarView.addDecorator(TodayDecorator())



        calendarView.setOnDateChangedListener { _, date, _ ->

            val selectedTrees = adapter.getSelectedTrees()

            if (selectedTrees.isNotEmpty()) {

                val dateString = "${date.year}-${date.month + 1}-${date.day}"



                selectedTrees.forEach { tree ->

                    val history = sprayHistory.getOrPut(tree.id) { mutableListOf() }

                    history.add(dateString)

                }



                Toast.makeText(

                    requireContext(),

                    "Sprayed ${selectedTrees.size} tree(s) on $dateString",

                    Toast.LENGTH_SHORT

                ).show()



                adapter.clearSelection()

            } else {

                Toast.makeText(
                    requireContext(),
                    "Select at least one tree first",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }


// Restore spray date (optional)

        val prefs = requireContext().getSharedPreferences("mango_prefs", 0)

        prefs.getString("spray_start_date", null)?.let { savedDate ->

            val parts = savedDate.split("-")

            if (parts.size == 3) {

                val savedCalendarDay = CalendarDay.from(

                    parts[0].toInt(),

                    parts[1].toInt() - 1,

                    parts[2].toInt()

                )

                highlightSprayDaysOnwards(savedCalendarDay)

                lastSelectedDate = savedCalendarDay

            }

        }


// Setup RecyclerView

        setupRecyclerView()



        return view

    }


    private fun setupRecyclerView() {

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)


        val trees = listOf(

            Tree("Tree 101", "2020/03/15", "Healthy", R.drawable.ic_healthy),

            Tree("Tree 102", "2021/05/20", "Flowering", R.drawable.ic_flower),

            Tree("Tree 103", "2019/11/02", "Harvest Ready", R.drawable.ic_harvest),

            Tree("Tree 104", "2022/01/10", "Diseased", R.drawable.ic_warning)

        )



        adapter = TreeAdapter(

            trees,

            onClick = { tree ->

                Toast.makeText(requireContext(), "Clicked: ${tree.id}", Toast.LENGTH_SHORT).show()

            },

            onLongClick = { tree ->

                Toast.makeText(requireContext(), "Long clicked: ${tree.id}", Toast.LENGTH_SHORT)
                    .show()

            }

        )



        recyclerView.adapter = adapter

    }


    private fun highlightSprayDaysOnwards(startDate: CalendarDay) {

        val creamDates = mutableListOf<CalendarDay>()

        val calendar = Calendar.getInstance().apply {

            set(startDate.year, startDate.month, startDate.day)

        }


// 110 days highlight (harvest)

        repeat(110) {

            creamDates.add(CalendarDay.from(calendar))

            calendar.add(Calendar.DAY_OF_MONTH, 1)

        }


        val harvestDate = creamDates.last().date

        val formattedHarvest = android.text.format.DateFormat.format("MMMM dd, yyyy", harvestDate)


// Next spray session (90–96 days)

        val greenDates = mutableListOf<CalendarDay>()

        val greenCalendar = Calendar.getInstance().apply {

            set(startDate.year, startDate.month, startDate.day)

            add(Calendar.DAY_OF_MONTH, 90)

        }

        repeat(7) {

            greenDates.add(CalendarDay.from(greenCalendar))

            greenCalendar.add(Calendar.DAY_OF_MONTH, 1)

        }

        val formattedNextSpray =

            android.text.format.DateFormat.format("MMMM dd, yyyy", greenDates.first().date)


// Yellow stage (30–60 days, warning period)

        val yellowDates = mutableListOf<CalendarDay>()

        val yellowCalendar = Calendar.getInstance().apply {

            set(startDate.year, startDate.month, startDate.day)

            add(Calendar.DAY_OF_MONTH, 30)

        }

        repeat(30) {

            yellowDates.add(CalendarDay.from(yellowCalendar))

            yellowCalendar.add(Calendar.DAY_OF_MONTH, 1)

        }


        val orangeDate = creamDates.last() // harvest


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

        tvNoLogs.visibility = View.GONE

    }


    private fun createDecorator(dates: List<CalendarDay>, drawableRes: Int): DayViewDecorator {

        return object : DayViewDecorator {

            override fun shouldDecorate(day: CalendarDay) = dates.contains(day)

            override fun decorate(view: DayViewFacade) {

                ContextCompat.getDrawable(requireContext(), drawableRes)?.let {

                    view.setBackgroundDrawable(it)

                }

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

            ContextCompat.getDrawable(requireContext(), R.drawable.today_circle)?.let {

                view.setBackgroundDrawable(it)

            }

        }

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.profile_menu, menu)

        val menuItem = menu.findItem(R.id.action_notification)

        val actionView = menuItem.actionView

        val badgeTextView = actionView?.findViewById<TextView>(R.id.badge_text_view)


        val unreadCount = 3

        badgeTextView?.apply {

            text = unreadCount.toString()

            visibility = if (unreadCount > 0) View.VISIBLE else View.GONE

        }



        actionView?.setOnClickListener { onOptionsItemSelected(menuItem) }



        super.onCreateOptionsMenu(menu, inflater)

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {

            R.id.action_logout -> {

                logoutUser()

                true

            }

            R.id.action_notification -> {

                showHarvestEndedNotification()

                true

            }

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



        if (ActivityCompat.checkSelfPermission(

                requireContext(),

                Manifest.permission.POST_NOTIFICATIONS

            ) != PackageManager.PERMISSION_GRANTED

        ) return



        NotificationManagerCompat.from(requireContext()).notify(1001, builder.build())

    }


    private fun vibrateDevice() {

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            val vm = requireContext().getSystemService(android.os.VibratorManager::class.java)

            vm?.defaultVibrator

        } else {

            @Suppress("DEPRECATION")

            requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator

        }



        vibrator?.vibrate(

            android.os.VibrationEffect.createOneShot(

                300,

                android.os.VibrationEffect.DEFAULT_AMPLITUDE

            )

        )

    }


    private fun logoutUser() {

        FirebaseAuth.getInstance().signOut()

        val intent = Intent(requireActivity(), LoginActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

    }
}
