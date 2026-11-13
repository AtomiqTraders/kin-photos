package com.grimm.kin.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest

/** Optional 4-digit PIN lock + "private mode" (blocks screenshots / hides in recents). */
object Security {
    private const val LOCK_AFTER_MS = 60_000L
    private lateinit var prefs: SharedPreferences

    var locked by mutableStateOf(false)
    var hasPin by mutableStateOf(false)
        private set
    var secureScreen by mutableStateOf(false)
        private set

    private var backgroundedAt = 0L

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("kin_security", Context.MODE_PRIVATE)
        hasPin = prefs.contains("pin")
        secureScreen = prefs.getBoolean("secure", false)
        locked = hasPin
    }

    fun setPin(pin: String) {
        prefs.edit().putString("pin", hash(pin)).apply()
        hasPin = true
    }

    fun removePin() {
        prefs.edit().remove("pin").apply()
        hasPin = false
        locked = false
    }

    fun check(pin: String): Boolean = prefs.getString("pin", null) == hash(pin)

    fun setSecure(on: Boolean) {
        prefs.edit().putBoolean("secure", on).apply()
        secureScreen = on
    }

    fun onBackground() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun onForeground() {
        if (hasPin && backgroundedAt > 0 && SystemClock.elapsedRealtime() - backgroundedAt > LOCK_AFTER_MS) {
            locked = true
        }
        backgroundedAt = 0L
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest("kin:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }
}
