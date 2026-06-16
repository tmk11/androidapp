package com.example.simpleapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SyncManager.Listener {

    private lateinit var db: NailDb
    private lateinit var prefs: Prefs
    private lateinit var sync: SyncManager
    private var selectedDay = Dates.todayKey()

    private lateinit var dateText: TextView
    private lateinit var grandTotalText: TextView
    private lateinit var syncStatus: TextView

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    private class TechRow(
        val total: TextView,
        val input: EditText,
        val chevron: TextView,
        val toggleLabel: TextView,
        val chips: ChipGroup,
        var expanded: Boolean = false
    )

    private val rows = LinkedHashMap<String, TechRow>()

    private val periodicHandler = Handler(Looper.getMainLooper())
    private val periodicSync = object : Runnable {
        override fun run() {
            sync.syncAsync(this@MainActivity)
            periodicHandler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        if (prefs.getPassword() == null) {
            startActivity(Intent(this, GateActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        db = NailDb(this)
        sync = SyncManager(db, prefs)

        dateText = findViewById(R.id.dateText)
        grandTotalText = findViewById(R.id.grandTotalText)
        syncStatus = findViewById(R.id.syncStatus)
        val techContainer = findViewById<LinearLayout>(R.id.techContainer)

        for (tech in NailDb.TECHS) {
            val card = layoutInflater.inflate(R.layout.item_tech_input, techContainer, false)
            card.findViewById<TextView>(R.id.techName).text = tech
            val input = card.findViewById<EditText>(R.id.amountInput)
            val row = TechRow(
                total = card.findViewById(R.id.techTotal),
                input = input,
                chevron = card.findViewById(R.id.techChevron),
                toggleLabel = card.findViewById(R.id.techToggleLabel),
                chips = card.findViewById(R.id.techChips)
            )
            rows[tech] = row
            card.findViewById<View>(R.id.addButton).setOnClickListener { addAmount(tech, input) }
            input.setOnEditorActionListener { _, _, _ -> addAmount(tech, input); true }
            card.findViewById<View>(R.id.techToggle).setOnClickListener {
                if (row.chips.childCount == 0) return@setOnClickListener
                row.expanded = !row.expanded
                applyExpand(row)
            }
            techContainer.addView(card)
        }

        findViewById<View>(R.id.changeDateButton).setOnClickListener { pickDate() }
        findViewById<View>(R.id.reportButton).setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }
        findViewById<View>(R.id.syncButton).setOnClickListener { sync.syncAsync(this) }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
        periodicHandler.post(periodicSync)
    }

    override fun onPause() {
        super.onPause()
        periodicHandler.removeCallbacks(periodicSync)
    }

    override fun onSyncStart() {
        syncStatus.text = getString(R.string.sync_syncing)
    }

    override fun onSyncDone(success: Boolean) {
        syncStatus.text =
            if (success) getString(R.string.sync_ok, timeFmt.format(Date()))
            else getString(R.string.sync_error)
        refresh()
    }

    private fun addAmount(tech: String, input: EditText) {
        val cents = Money.parseToCents(input.text.toString())
        if (cents == null || cents <= 0L) {
            Toast.makeText(this, getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show()
            return
        }
        db.addLocal(tech, cents, selectedDay)
        input.setText("")
        input.clearFocus()
        rows[tech]?.expanded = true
        refresh()
        sync.syncAsync(this)
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

        var grand = 0L
        val customerLabel = getString(R.string.customer)
        for (tech in NailDb.TECHS) {
            val row = rows[tech] ?: continue
            val entries = db.entriesForTechDay(tech, selectedDay)

            val techTotal = entries.sumOf { it.cents }
            grand += techTotal
            row.total.text = Money.format(techTotal)

            row.chips.removeAllViews()
            if (entries.isEmpty()) {
                row.toggleLabel.text = getString(R.string.no_customers)
                row.chevron.visibility = View.GONE
                row.expanded = false
            } else {
                row.chevron.visibility = View.VISIBLE
                row.toggleLabel.text = getString(R.string.customers_count, entries.size)
                entries.forEachIndexed { index, entry ->
                    val label = "$customerLabel ${index + 1}"
                    val chip = Chip(this).apply {
                        text = Money.format(entry.cents)
                        isCloseIconVisible = true
                        isCheckable = false
                        setOnClickListener { confirmDelete(entry, label) }
                        setOnCloseIconClickListener { confirmDelete(entry, label) }
                    }
                    row.chips.addView(chip)
                }
            }
            applyExpand(row)
        }
        grandTotalText.text = Money.format(grand)
    }

    private fun applyExpand(row: TechRow) {
        val show = row.expanded && row.chips.childCount > 0
        row.chips.visibility = if (show) View.VISIBLE else View.GONE
        row.chevron.text = if (show) "▾" else "▸"
    }

    private fun confirmDelete(entry: Entry, label: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_delete_title))
            .setMessage(
                "${entry.tech} · $label\n${Money.format(entry.cents)}\n\n" +
                    getString(R.string.cannot_undo)
            )
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                db.softDelete(entry.clientUuid)
                refresh()
                sync.syncAsync(this)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
