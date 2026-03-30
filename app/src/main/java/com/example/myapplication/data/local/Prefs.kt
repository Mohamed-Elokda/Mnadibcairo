package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Prefs {
    private const val PREFS_NAME = "App_Prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USERNAME = "profiles"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_IS_FIRST_SYNC = "is_first_sync_done"

    // دالة للتحقق: هل تمت المزامنة الأولى؟
    fun isFirstSyncDone(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_FIRST_SYNC, false)
    }

    // دالة لحفظ الحالة بعد اكتمال المزامنة بنجاح
    fun setFirstSyncDone(context: Context, isDone: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_FIRST_SYNC, isDone).apply()
    }
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }


    fun setLoggedIn(context: Context, isLoggedIn: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_IS_LOGGED_IN, isLoggedIn) }
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }


    fun saveUserData(context: Context, id: String, username: String) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_ID, id)
            putString(KEY_USERNAME, username)
            apply()
        }
    }


    fun getUsername(context: Context): String? {
        return getPrefs(context).getString(KEY_USERNAME, null)
    }
fun getUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null)
    }


    fun clear(context: Context) {
        getPrefs(context).edit { clear() }
    }
}