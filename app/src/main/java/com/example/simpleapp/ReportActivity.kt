package com.example.simpleapp

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

/** Revenue reports per technician: by chosen day, chosen month, or all time. Each is shareable. */
class ReportActivity : AppCompatActivity() {

    private lateinit var db: NailDb

    private var selectedDay = Dates.todayKey()
    private var selMonth = 1
    private var selYear = 2026

    private lateinit var daySelectedText: TextView
    private lateinit var monthSelectedText: TextView
    private lateinit var dayRows: LinearLayout
    private lateinit var monthRows: LinearLayout
    private lateinit var allRows: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        db = NailDb(this)

        val now = Calendar.getInstance()
        selYear = now.get(Calendar.YEAR)
        selMonth = now.get(Calendar.MONTH) + 1

        daySelectedText = findViewById(R.id.daySelectedText)
        monthSelectedText = findViewById(R.id.monthSelectedText)
        dayRows = findViewById(R.id.dayRows)
        monthRows = findViewById(R.id.monthRows)
        allRows = findViewById(R.id.allRows)

        findViewById<View>(R.id.closeButton).setOnClickListener { finish() }
        findViewById<View>(R.id.btnPickDay).setOnClickListener { pickDay() }
        findViewById<View>(R.id.btnPickMonth).setOnClickListener { pickMonth() }

        findViewById<View>(R.id.btnShareDay).setOnClickListener {
            share(buildText("Ngày ${Dates.displayShort(selectedDay)}", db.totalsByTechLike(selectedDay)))
        }
        findViewById<View>(R.id.btnShareMonth).setOnClickListener {
            share(
                buildText(
                    "Tháng ${Dates.monthDisplay(selYear, selMonth)}",
                    db.totalsByTechLike(Dates.monthPatternOf(selYear, selMonth))
                )
            )
        }
        findViewById<View>(R.id.btnShareAll).setOnClickListener {
            share(buildText(getString(R.string.report_all), db.totalsByTechAll()))
        }

        renderDay()
        renderMonth()
        renderAll()

        // Pull the latest data from the server, then re-render.
        SyncManager(db, Prefs(this)).syncAsync(object : SyncManager.Listener {
            override fun onSyncStart() {}
            override fun onSyncDone(success: Boolean) {
                renderDay()
                renderMonth()
                renderAll()
            }
        })
    }

    private fun renderDay() {
        daySelectedText.text = Dates.displayShort(selectedDay)
        fillRows(dayRows, db.totalsByTechLike(selectedDay))
    }

    private fun renderMonth() {
        monthSelectedText.text = Dates.monthDisplay(selYear, selMonth)
        fillRows(monthRows, db.totalsByTechLike(Dates.monthPatternOf(selYear, selMonth)))
    }

    private fun renderAll() {
        fillRows(allRows, db.totalsByTechAll())
    }

    private fun fillRows(container: LinearLayout, totals: Map<String, Long>) {
        container.removeAllViews()
        var grand = 0L
        for (tech in NailDb.TECHS) {
            val cents = totals[tech] ?: 0L
            grand += cents
            container.addView(makeRow(tech, Money.format(cents), bold = false))
        }
        container.addView(makeDivider())
        container.addView(makeRow(getString(R.string.total), Money.format(grand), bold = true))
    }

    private fun pickDay() {
        val cal = Calendar.getInstance()
        cal.time = Dates.parseKey(selectedDay)
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val c = Calendar.getInstance()
                c.set(year, month, dayOfMonth, 0, 0, 0)
                selectedDay = Dates.keyFromCalendar(c)
                renderDay()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickMonth() {
        val monthPicker = NumberPicker(this).apply {
            minValue = 1
            maxValue = 12
            displayedValues = Array(12) { "Tháng ${it + 1}" }
            value = selMonth
            wrapSelectorWheel = false
        }
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearPicker = NumberPicker(this).apply {
            minValue = 2020
            maxValue = thisYear + 1
            value = selYear
            wrapSelectorWheel = false
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), 0)
            addView(monthPicker, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(yearPicker, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pick_month))
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                selMonth = monthPicker.value
                selYear = yearPicker.value
                renderMonth()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun buildText(period: String, totals: Map<String, Long>): String {
        val sb = StringBuilder()
        sb.append("💅 ").append(getString(R.string.app_name)).append("\n")
        sb.append(period).append("\n\n")
        var grand = 0L
        for (tech in NailDb.TECHS) {
            val cents = totals[tech] ?: 0L
            grand += cents
            sb.append("• ").append(tech).append(": ").append(Money.format(cents)).append("\n")
        }
        sb.append("\n").append(getString(R.string.total)).append(": ").append(Money.format(grand))
        return sb.toString()
    }

    private fun share(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.export_report)))
    }

    private fun makeRow(label: String, value: String, bold: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val pad = dp(8)
            setPadding(0, pad, 0, pad)
        }
        val style = if (bold) Typeface.BOLD else Typeface.NORMAL
        val labelView = TextView(this).apply {
            text = label
            textSize = 16f
            setTypeface(typeface, style)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valueView = TextView(this).apply {
            text = value
            textSize = 16f
            gravity = Gravity.END
            setTypeface(typeface, style)
        }
        row.addView(labelView)
        row.addView(valueView)
        return row
    }

    private fun makeDivider(): View {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        v.setBackgroundColor(0xFFE0E0E0.toInt())
        return v
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
