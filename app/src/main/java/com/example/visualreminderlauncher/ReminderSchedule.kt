package com.example.visualreminderlauncher

import org.json.JSONArray
import org.json.JSONObject

data class ReminderSchedule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val repeatDaily: Boolean = true,
    val enabled: Boolean = true
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("startHour", startHour)
            put("startMinute", startMinute)
            put("endHour", endHour)
            put("endMinute", endMinute)
            put("repeatDaily", repeatDaily)
            put("enabled", enabled)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ReminderSchedule {
            return ReminderSchedule(
                id = json.getString("id"),
                startHour = json.getInt("startHour"),
                startMinute = json.getInt("startMinute"),
                endHour = json.getInt("endHour"),
                endMinute = json.getInt("endMinute"),
                repeatDaily = json.optBoolean("repeatDaily", true),
                enabled = json.optBoolean("enabled", true)
            )
        }

        fun toJsonArray(schedules: List<ReminderSchedule>): String {
            val array = JSONArray()
            schedules.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }

        fun fromJsonArray(jsonString: String): List<ReminderSchedule> {
            val list = mutableListOf<ReminderSchedule>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
            } catch (e: Exception) {}
            return list
        }
    }
}
