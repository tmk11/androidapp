package com.example.simpleapp

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Date helpers. The canonical "day key" is an ISO string like 2026-06-15. */
object Dates {

    private val vi = Locale("vi")
    private val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val longFmt = SimpleDateFormat("EEEE, dd/MM/yyyy", vi)
    private val shortFmt = SimpleDateFormat("dd/MM/yyyy", vi)

    fun todayKey(): String = keyFmt.format(Date())

    fun keyFromCalendar(cal: Calendar): String = keyFmt.format(cal.time)

    fun parseKey(key: String): Date = keyFmt.parse(key) ?: Date()

    fun displayLong(key: String): String = longFmt.format(parseKey(key))

    fun displayShort(key: String): String = shortFmt.format(parseKey(key))

    /** SQL LIKE pattern matching the month of [key], e.g. "2026-06-%". */
    fun monthPattern(key: String): String = key.substring(0, 7) + "-%"
}
