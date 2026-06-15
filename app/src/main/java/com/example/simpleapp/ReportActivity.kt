package com.example.simpleapp

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Shows revenue totals per technician for today, this month and all time. */
class ReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        findViewById<View>(R.id.closeButton).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.reportContainer)
        val db = NailDb(this)
        val today = Dates.todayKey()

        addSection(container, "Hôm nay · ${Dates.displayShort(today)}", db.totalsByTechLike(today))
        addSection(container, "Tháng này", db.totalsByTechLike(Dates.monthPattern(today)))
        addSection(container, "Tất cả thời gian", db.totalsByTechAll())
    }

    private fun addSection(parent: LinearLayout, title: String, totals: Map<String, Long>) {
        val card = layoutInflater.inflate(R.layout.item_report_section, parent, false)
        card.findViewById<TextView>(R.id.sectionTitle).text = title
        val rows = card.findViewById<LinearLayout>(R.id.sectionRows)

        var grand = 0L
        for (tech in NailDb.TECHS) {
            val cents = totals[tech] ?: 0L
            grand += cents
            rows.addView(makeRow(tech, Money.format(cents), bold = false))
        }
        rows.addView(makeDivider())
        rows.addView(makeRow(getString(R.string.total), Money.format(grand), bold = true))

        parent.addView(card)
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
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        )
        v.setBackgroundColor(0xFFE0E0E0.toInt())
        return v
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
