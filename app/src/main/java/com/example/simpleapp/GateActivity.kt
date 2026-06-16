package com.example.simpleapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

/**
 * First-launch gate. If no password is stored on this device, it talks to the
 * server: if the shop has no password yet, the first device creates it;
 * otherwise the user must enter the existing shared password. Once verified the
 * password is saved locally and the main screen opens.
 */
class GateActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private lateinit var titleText: TextView
    private lateinit var statusText: TextView
    private lateinit var passwordInput: EditText
    private lateinit var continueButton: Button
    private lateinit var progress: ProgressBar

    private var passwordExists = false
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        if (prefs.getPassword() != null) {
            goMain()
            return
        }
        setContentView(R.layout.activity_gate)
        titleText = findViewById(R.id.gateTitle)
        statusText = findViewById(R.id.gateStatus)
        passwordInput = findViewById(R.id.gatePassword)
        continueButton = findViewById(R.id.gateContinue)
        progress = findViewById(R.id.gateProgress)

        continueButton.setOnClickListener { submit() }
        detectState()
    }

    private fun detectState() {
        setBusy(true, getString(R.string.gate_checking))
        io.execute {
            val set = Api.isPasswordSet()
            main.post {
                if (set == null) {
                    setBusy(false, getString(R.string.gate_no_network))
                } else {
                    passwordExists = set
                    ready = true
                    titleText.text =
                        getString(if (set) R.string.gate_enter else R.string.gate_create)
                    setBusy(false, null)
                }
            }
        }
    }

    private fun submit() {
        if (!ready) {
            detectState()
            return
        }
        val pw = passwordInput.text.toString().trim()
        if (pw.isEmpty()) {
            Toast.makeText(this, getString(R.string.gate_empty), Toast.LENGTH_SHORT).show()
            return
        }
        setBusy(true, getString(R.string.gate_checking))
        io.execute {
            val result = if (passwordExists) Api.checkPassword(pw) else Api.initPassword(pw)
            main.post {
                when {
                    result == null -> setBusy(false, getString(R.string.gate_no_network))
                    result -> {
                        prefs.setPassword(pw)
                        goMain()
                    }
                    passwordExists -> setBusy(false, getString(R.string.gate_wrong))
                    else -> {
                        // Another device set the password first; switch to entry mode.
                        passwordExists = true
                        titleText.text = getString(R.string.gate_enter)
                        setBusy(false, null)
                    }
                }
            }
        }
    }

    private fun setBusy(busy: Boolean, status: String?) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        continueButton.isEnabled = !busy
        passwordInput.isEnabled = !busy
        statusText.text = status ?: ""
        statusText.visibility = if (status.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
