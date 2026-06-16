package com.example.simpleapp

import android.content.Context

/** Stores the shop's shared password locally so it is only entered once per device. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("nail_prefs", Context.MODE_PRIVATE)

    fun getPassword(): String? = sp.getString(KEY_PASSWORD, null)

    fun setPassword(password: String) {
        sp.edit().putString(KEY_PASSWORD, password).apply()
    }

    fun clear() {
        sp.edit().clear().apply()
    }

    private companion object {
        const val KEY_PASSWORD = "password"
    }
}
