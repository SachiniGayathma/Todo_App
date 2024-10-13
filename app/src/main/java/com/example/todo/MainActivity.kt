package com.example.todo


import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent

import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tasksAdapter: ArrayAdapter<String>
    private lateinit var taskListView: ListView
    private var taskList: MutableList<String> = mutableListOf()
    private var selectedReminderTime: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()


        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("taskPrefs", Context.MODE_PRIVATE)

        // Set up ListView and Adapter
        taskListView = findViewById(R.id.taskListView)
        tasksAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, taskList)
        taskListView.adapter = tasksAdapter

        // Load saved tasks
        loadTasks()

        // Set up "Add Task" button click listener
        val addTaskButton = findViewById<Button>(R.id.addTaskButton)
        addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        // Set task click listener to show details
        taskListView.setOnItemClickListener { _, _, position, _ ->
            showTaskDetailsDialog(position)
        }


    }

    private fun showAddTaskDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Task")

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val taskInput = dialogLayout.findViewById<TextInputEditText>(R.id.taskInput)
        val dateButton = dialogLayout.findViewById<Button>(R.id.dateButton)
        val timeButton = dialogLayout.findViewById<Button>(R.id.timeButton)
        val reminderButton = dialogLayout.findViewById<Button>(R.id.reminderButton)

        var selectedDate: String? = null
        var selectedTime: String? = null
        var selectedReminderTime: String? = null

        // DatePicker
        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this, { _, year, month, dayOfMonth ->
                    selectedDate = "$dayOfMonth/${month + 1}/$year"
                    dateButton.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // TimePicker
        timeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this, { _, hourOfDay, minute ->
                    selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                    timeButton.text = selectedTime
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
        reminderButton.setOnClickListener {
            try {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this, { _, year, month, dayOfMonth ->
                        val selectedDate = "$dayOfMonth/${month + 1}/$year"
                        TimePickerDialog(
                            this, { _, hourOfDay, minute ->
                                val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                                selectedReminderTime = "$selectedDate at $selectedTime"

                                val alarmCalendar = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                    set(Calendar.SECOND, 0)
                                }

                                if (alarmCalendar.timeInMillis > System.currentTimeMillis()) {
                                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    val intent = Intent(this, AlarmReceiver::class.java).apply {
                                        putExtra("reminder_message", "Reminder set for $selectedDate at $selectedTime")
                                        putExtra("task_name", taskInput.text.toString())
                                    }
                                    val pendingIntent = PendingIntent.getBroadcast(
                                        this,
                                        0,
                                        intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )

                                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarmCalendar.timeInMillis, pendingIntent)

                                    // Debug Toast
                                    Toast.makeText(this, "Setting reminder for $selectedDate at $selectedTime", Toast.LENGTH_SHORT).show()

                                    // Update button text
                                    reminderButton.text = "Reminder set for $selectedDate at $selectedTime"
                                } else {
                                    Toast.makeText(this, "Selected time is in the past", Toast.LENGTH_SHORT).show()
                                }
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }



        }





        builder.setView(dialogLayout)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val task = taskInput.text.toString().trim()

            if (task.isNotEmpty()) {
                saveTask(task, selectedDate, selectedTime, selectedReminderTime)
                Toast.makeText(this, "New task added successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a task!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, TaskUpcomingWidget::class.java))
        if (appWidgetIds.isNotEmpty()) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(this, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun saveTask(task: String, date: String?, time: String?, reminderTime: String?) {
        val taskDetails = "$task\nDate: ${date ?: "Not Fixed"}\nTime: ${time ?: "Not Fixed"}\nReminder: ${reminderTime ?: "Not Fixed"}"
        taskList.add(taskDetails)
        saveTasksToPreferences()

        // Update ListView
        tasksAdapter.notifyDataSetChanged()
        updateWidget()


    }


    private fun loadTasks() {
        val savedTasks = sharedPreferences.getStringSet("tasks", null)
        if (savedTasks != null) {
            taskList.addAll(savedTasks)
            tasksAdapter.notifyDataSetChanged()
        }

    }

    private fun showTaskDetailsDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Task Details")

        val taskDetails = taskList[position]
        val timerView = TextView(this) // Create a TextView for displaying the timer
        timerView.textSize = 24f
        val elapsedTime = StringBuilder("00:00:00") // To hold the elapsed time
        var isTimerRunning = false
        var startTime: Long = 0
        var handler = Handler()

        // Timer Runnable to update the elapsed time
        val timerRunnable = object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val hours = (elapsed / 3600000).toInt()
                    val minutes = (elapsed / 60000 % 60).toInt()
                    val seconds = (elapsed / 1000 % 60).toInt()
                    elapsedTime.clear()
                    elapsedTime.append(String.format("%02d:%02d:%02d", hours, minutes, seconds))
                    timerView.text = elapsedTime.toString()
                    handler.postDelayed(this, 1000) // Update every second
                }
            }
        }

        // Start/Stop buttons
        val startButton = Button(this).apply {
            text = "Start Timer"
            setOnClickListener {
                if (!isTimerRunning) {
                    startTime = System.currentTimeMillis()
                    isTimerRunning = true
                    handler.post(timerRunnable) // Start the timer
                    text = "Stop Timer" // Change button text to Stop
                } else {
                    isTimerRunning = false
                    text = "Start Timer" // Change button text back to Start
                }
            }
        }

        // Displaying the task details and timer
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = taskDetails
                textSize = 18f
            })
            addView(timerView) // Add the timer view to the layout
            addView(startButton) // Add the start/stop button
        }

        builder.setView(dialogLayout)

        builder.setPositiveButton("Update") { dialog, _ ->
            updateTask(position)
            dialog.dismiss()
        }
        builder.setNegativeButton("Delete") { dialog, _ ->
            deleteTask(position)
            dialog.dismiss()
        }
        builder.setNeutralButton("OK") { dialog, _ -> dialog.dismiss() }

        builder.show()
    }


    private fun updateTask(position: Int) {
        val currentTask = taskList[position]

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update Task")

        // Inflate the dialog layout
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val taskInput = dialogLayout.findViewById<TextInputEditText>(R.id.taskInput)
        val dateButton = dialogLayout.findViewById<Button>(R.id.dateButton)
        val timeButton = dialogLayout.findViewById<Button>(R.id.timeButton)
        val reminderButton = dialogLayout.findViewById<Button>(R.id.reminderButton)

        // Split current task details for pre-filling
        val taskDetails = currentTask.split("\n")
        taskInput.setText(taskDetails[0]) // Task name
        dateButton.text = taskDetails[1].substringAfter("Date: ").trim()
        timeButton.text = taskDetails[2].substringAfter("Time: ").trim()
        reminderButton.text = taskDetails[3].substringAfter("Reminder: ").trim()

        // DatePicker for updating date
        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this, { _, year, month, dayOfMonth ->
                    val selectedDate = "$dayOfMonth/${month + 1}/$year"
                    dateButton.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()

            updateWidget()

        }

        // TimePicker for updating time
        timeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this, { _, hourOfDay, minute ->
                    val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                    timeButton.text = selectedTime
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        reminderButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this, { _, year, month, dayOfMonth ->
                    val selectedDate = "$dayOfMonth/${month + 1}/$year"
                    // After selecting the date, show a time picker
                    TimePickerDialog(
                        this, { _, hourOfDay, minute ->
                            val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                            // Combine date and time
                            selectedReminderTime = "$selectedDate at $selectedTime"
                            reminderButton.text = "Reminder set for $selectedDate at $selectedTime"
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }



        builder.setView(dialogLayout)

        builder.setPositiveButton("Update") { dialog, _ ->
            val updatedTask = taskInput.text.toString().trim()
            val updatedDate = dateButton.text.toString()
            val updatedTime = timeButton.text.toString()
            val updatedReminder = reminderButton.text.toString()

            if (updatedTask.isNotEmpty()) {
                // Update task in the list
                val updatedTaskDetails = "$updatedTask\nDate: $updatedDate\nTime: $updatedTime\nReminder: $updatedReminder"
                taskList[position] = updatedTaskDetails

                // Save updated task back to SharedPreferences
                saveTasksToPreferences()
                tasksAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Task updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a task!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
        updateWidget()

    }

    private fun deleteTask(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Task")
        builder.setMessage("Are you sure you want to delete this task?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            // Remove the task from the list
            taskList.removeAt(position)

            // Save updated task list back to SharedPreferences
            saveTasksToPreferences()

            // Notify adapter of the change
            tasksAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Task deleted successfully!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }

        builder.show()
        
    }

    private fun saveTasksToPreferences() {
        val editor = sharedPreferences.edit()
        editor.putStringSet("tasks", taskList.toMutableSet())
        editor.apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Reminder"
            val descriptionText = "Channel for task reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("task_reminder_channel", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }










}