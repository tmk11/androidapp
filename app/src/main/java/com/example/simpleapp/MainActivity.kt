package com.example.simpleapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

/**
 * A minimal single-screen app: a title, a counter, and a button that
 * increments the counter each time it is tapped.
 */
class MainActivity : Activity() {

    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val counterText = findViewById<TextView>(R.id.counterText)
        val tapButton = findViewById<Button>(R.id.tapButton)

        tapButton.setOnClickListener {
            count++
            counterText.text = getString(R.string.count_value, count)
        }
    }
}
