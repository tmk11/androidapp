package com.example.simpleapp

import android.content.Context

/**
 * Per-device settings: the shared shop password (entered once) and the name of
 * the person/phone using this device (tagged onto entries created here).
 */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("nail_prefs", Context.MODE_PRIVATE)

    fun getPassword(): String? = sp.getString(KEY_PASSWORD, null)

    fun setPassword(password: String) {
        sp.edit().putString(KEY_PASSWORD, password).apply()
    }

    fun getDeviceName(): String? = sp.getString(KEY_DEVICE_NAME, null)

    fun setDeviceName(name: String) {
        sp.edit().putString(KEY_DEVICE_NAME, name).apply()
    }

    fun clear() {
        sp.edit().clear().apply()
    }

    private companion object {
        const val KEY_PASSWORD = "password"
        const val KEY_DEVICE_NAME = "device_name"
    }
}
