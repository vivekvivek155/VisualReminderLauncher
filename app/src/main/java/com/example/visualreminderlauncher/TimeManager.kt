package com.example.visualreminderlauncher

import android.content.Context
import java.util.Calendar

object TimeManager {

    private const val PREF = "reminder_prefs"
    private const val START_HOUR = "start_hour"
    private const val START_MIN = "start_min"
    private const val END_HOUR = "end_hour"
    private const val END_MIN = "end_min"

    fun saveStartTime(context: Context, hour: Int, minute: Int) {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        pref.edit()
            .putInt(START_HOUR, hour)
            .putInt(START_MIN, minute)
            .apply()
    }

    fun saveEndTime(context: Context, hour: Int, minute: Int) {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        pref.edit()
            .putInt(END_HOUR, hour)
            .putInt(END_MIN, minute)
            .apply()
    }

    fun getStartTime(context: Context): Pair<Int, Int> {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return Pair(pref.getInt(START_HOUR, 20), pref.getInt(START_MIN, 0))
    }

    fun getEndTime(context: Context): Pair<Int, Int> {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return Pair(pref.getInt(END_HOUR, 22), pref.getInt(END_MIN, 0))
    }

    fun isReminderTime(context: Context): Boolean {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        
        val startHour = pref.getInt(START_HOUR, 20)
        val startMin = pref.getInt(START_MIN, 0)
        val endHour = pref.getInt(END_HOUR, 22)
        val endMin = pref.getInt(END_MIN, 0)

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        // Convert everything to seconds for higher precision
        val now = hour * 3600 + minute * 60 + second
        val start = startHour * 3600 + startMin * 60
        val end = endHour * 3600 + endMin * 60

        return if (start <= end) {
            // Normal range: start at :00, end at :00
            now >= start && now < end
        } else {
            // Overnight range
            now >= start || now < end
        }
    }
}
