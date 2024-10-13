package com.example.todo

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import android.content.SharedPreferences

/**
 * Implementation of App Widget functionality.
 */
class TaskUpcomingWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all active widgets
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Optional: Any special functionality for when the widget is first created
    }

    override fun onDisabled(context: Context) {
        // Optional: Any cleanup for when the last widget is removed
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
)  {
    val sharedPreferences = context.getSharedPreferences("taskPrefs", Context.MODE_PRIVATE)
    val tasks = sharedPreferences.getStringSet("tasks", emptySet())?.toList() ?: listOf("No upcoming tasks")

    val views = RemoteViews(context.packageName, R.layout.task_upcoming_widget)

    // Update the TextViews for tasks
    for (i in tasks.indices) {
        val taskId = context.resources.getIdentifier("task_name_$i", "id", context.packageName)
        views.setTextViewText(taskId, tasks[i])
    }

    // Handle the case where there are no tasks
    if (tasks.isEmpty()) {
        views.setTextViewText(R.id.task_name_0, "No upcoming tasks")
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}


