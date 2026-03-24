package com.example.visualreminderlauncher

import android.content.Context
import java.util.Calendar

object TimeManager {

    fun isReminderTime(context: Context): Boolean {
        val schedules = UserPreferences.getReminderSchedules(context).filter { it.enabled }
        if (schedules.isEmpty()) return false

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val now = hour * 3600 + minute * 60 + second

        for (schedule in schedules) {
            val start = schedule.startHour * 3600 + schedule.startMinute * 60
            val end = schedule.endHour * 3600 + schedule.endMinute * 60

            val active = if (start <= end) {
                now >= start && now < end
            } else {
                now >= start || now < end
            }

            if (active) return true
        }
        return false
    }
}