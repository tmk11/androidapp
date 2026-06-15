package com.example.simpleapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var db: NailDb
    private var selectedDay = Dates.todayKey()

    private lateinit var dateText: TextView
    private lateinit var grandTotalText: TextView
    private lateinit var entriesContainer: LinearLayout
    private lateinit var emptyText: TextView

    private class TechRow(val total: TextView, val input: EditText)
    private val rows = LinkedHashMap<String, TechRow>()

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = NailDb(this)

        dateText = findViewById(R.id.dateText)
        grandTotalText = findViewById(R.id.grandTotalText)
        entriesContainer = findViewById(R.id.entriesContainer)
        emptyText = findViewById(R.id.emptyText)
        val techContainer = findViewById<LinearLayout>(R.id.techContainer)

        for (tech in NailDb.TECHS) {
            val card = layoutInflater.inflate(R.layout.item_tech_input, techContainer, false)
            card.findViewById<TextView>(R.id.techName).text = tech
            val total = card.findViewById<TextView>(R.id.techTotal)
            val input = card.findViewById<EditText>(R.id.amountInput)
            card.findViewById<View>(R.id.addButton).setOnClickListener { addAmount(tech, input) }
            input.setOnEditorActionListener { _, _, _ -> addAmount(tech, input); true }
            rows[tech] = TechRow(total, input)
            techContainer.addView(card)
        }

        findViewById<View>(R.id.changeDateButton).setOnClickListener { pickDate() }
        findViewById<View>(R.id.reportButton).setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun addAmount(tech: String, input: EditText) {
        val cents = Money.parseToCents(input.text.toString())
        if (cents == null || cents <= 0L) {
            Toast.makeText(this, getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show()
            return
        }
        db.addEntry(tech, cents, selectedDay)
        input.setText("")
        input.clearFocus()
        refresh()
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        cal.time = Dates.parseKey(selectedDay)
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance()
                picked.set(year, month, dayOfMonth, 0, 0, 0)
                selectedDay = Dates.keyFromCalendar(picked)
                refresh()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun refresh() {
        dateText.text = Dates.displayLong(selectedDay)

        val totals = db.totalsByTechLike(selectedDay)
        var grand = 0L
        for (tech in NailDb.TECHS) {
            val cents = totals[tech] ?: 0L
            grand += cents
            rows[tech]?.total?.text = Money.format(cents)
        }
        grandTotalText.text = Money.format(grand)

        entriesContainer.removeAllViews()
        val entries = db.entriesForDay(selectedDay)
        emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        for (entry in entries) {
            val row = layoutInflater.inflate(R.layout.item_entry, entriesContainer, false)
            row.findViewById<TextView>(R.id.entryText).text =
                "${entry.tech}  ·  ${Money.format(entry.cents)}  ·  ${timeFmt.format(Date(entry.createdAt))}"
            row.findViewById<View>(R.id.deleteButton).setOnClickListener { confirmDelete(entry) }
            entriesContainer.addView(row)
        }
    }

    private fun confirmDelete(entry: Entry) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setMessage("${entry.tech} · ${Money.format(entry.cents)}")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                db.deleteEntry(entry.id)
                refresh()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
