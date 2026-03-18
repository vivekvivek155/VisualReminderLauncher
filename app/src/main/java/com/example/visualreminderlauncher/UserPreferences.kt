package com.example.visualreminderlauncher

import android.content.Context

object UserPreferences {

    private const val PREF = "user_pref"
    private const val SELECTED_APPS = "selected_apps"
    private const val WALLPAPER_URI = "wallpaper_uri"
    private const val APP_ORDER = "app_order"
    private const val WARNING_MESSAGE = "warning_message"

    fun saveSelectedApps(context: Context, apps: Set<String>) {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        pref.edit().putStringSet(SELECTED_APPS, apps).apply()
    }

    fun getSelectedApps(context: Context): Set<String> {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return pref.getStringSet(SELECTED_APPS, emptySet()) ?: emptySet()
    }

    fun saveWallpaperUri(context: Context, uri: String) {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        pref.edit().putString(WALLPAPER_URI, uri).apply()
    }

    fun getWallpaperUri(context: Context): String? {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return pref.getString(WALLPAPER_URI, null)
    }

    fun saveAppOrder(context: Context, order: List<String>) {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        pref.edit().putString(APP_ORDER, order.joinToString(",")).apply()
    }

    fun getAppOrder(context: Context): List<String>? {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val orderStr = pref.getString(APP_ORDER, null)
        return orderStr?.split(",")?.filter { it.isNotEmpty() }
    }

    fun saveWarningMessage(context: Context, message: String) {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        pref.edit().putString(WARNING_MESSAGE, message).apply()
    }

    fun getWarningMessage(context: Context): String {
        val pref = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return pref.getString(WARNING_MESSAGE, "Do you really need to open %s?") ?: "Do you really need to open %s?"
    }
}