package com.example.simpleapp

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/** Helpers for working with Euro amounts stored internally as integer cents. */
object Money {

    private val euroFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)

    /** e.g. 1250 -> "12,50 €" */
    fun format(cents: Long): String = euroFormat.format(cents / 100.0)

    /**
     * Parse free-form user input into cents. Accepts "12", "12,5", "12.50",
     * "1.234,56", "1,234.56". The last comma/dot is treated as the decimal
     * separator; other separators are dropped. Returns null when invalid.
     */
    fun parseToCents(input: String): Long? {
        val t = input.trim().replace("€", "").replace(" ", "").replace(" ", "")
        if (t.isEmpty()) return null
        val decimalPos = max(t.lastIndexOf(','), t.lastIndexOf('.'))
        return try {
            if (decimalPos == -1) {
                val digits = t.filter { it.isDigit() }
                if (digits.isEmpty()) null else digits.toLong() * 100
            } else {
                val intDigits = t.substring(0, decimalPos).filter { it.isDigit() }
                val fracDigits = (t.substring(decimalPos + 1).filter { it.isDigit() } + "00").substring(0, 2)
                val euros = if (intDigits.isEmpty()) 0L else intDigits.toLong()
                euros * 100 + fracDigits.toLong()
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
