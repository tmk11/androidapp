package com.example.simpleapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * An interactive fireworks playground drawn entirely with [Canvas].
 *
 *  - Tap            -> launch a rocket that rises and bursts into colourful sparks
 *  - Drag           -> paint a trail of glowing sparkles
 *  - Shake the phone -> fire a whole salvo at once
 *
 * The night sky (twinkling stars + a glowing moon) is redrawn every frame on the
 * hardware canvas, while the fireworks are rendered onto a persistent bitmap whose
 * alpha is gently faded each frame to produce smooth motion trails.
 */
class FireworksView(context: Context) : View(context),
    Choreographer.FrameCallback, SensorEventListener {

    private class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var life: Float, val maxLife: Float,
        val color: Int, val size: Float,
        val gravity: Float, val twinkle: Boolean
    )

    private class Rocket(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        val targetY: Float, val hue: Float
    )

    private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val baseA: Int)

    private val particles = ArrayList<Particle>()
    private val rockets = ArrayList<Rocket>()
    private val stars = ArrayList<Star>()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fadePaint = Paint().apply {
        color = Color.BLACK
        alpha = 30
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    private val bgPaint = Paint()
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val moonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(220, 220, 255)
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 0f, 0f, Color.BLACK)
    }

    private var buffer: Bitmap? = null
    private var bufferCanvas: Canvas? = null

    private var moonX = 0f
    private var moonY = 0f
    private var moonR = 0f

    private var lastFrameNanos = 0L
    private var elapsed = 0f
    private var interacted = false
    private var interactAt = 0f
    private var lastShake = -10f

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val hintLine1 = context.getString(R.string.hint_line1)
    private val hintLine2 = context.getString(R.string.hint_line2)

    init {
        isFocusable = true
        keepScreenOn = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        buffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bufferCanvas = Canvas(buffer!!)

        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.rgb(10, 12, 46), Color.rgb(2, 2, 14),
            Shader.TileMode.CLAMP
        )

        stars.clear()
        val count = (w * h / 9000).coerceIn(60, 220)
        repeat(count) {
            stars.add(
                Star(
                    Random.nextFloat() * w,
                    Random.nextFloat() * h * 0.9f,
                    Random.nextFloat() * 1.8f + 0.6f,
                    Random.nextFloat() * 6.283f,
                    Random.nextInt(70, 200)
                )
            )
        }

        moonX = w * 0.80f
        moonY = h * 0.15f
        moonR = min(w, h) * 0.055f
        moonGlowPaint.shader = RadialGradient(
            moonX, moonY, moonR * 3.4f,
            Color.argb(80, 220, 230, 255), Color.argb(0, 220, 230, 255),
            Shader.TileMode.CLAMP
        )

        textPaint.textSize = min(w, h) * 0.05f
        subTextPaint.textSize = min(w, h) * 0.032f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(this)
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(this)
        sensorManager?.unregisterListener(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (lastFrameNanos == 0L) lastFrameNanos = frameTimeNanos
        var dt = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = frameTimeNanos
        if (dt > 0.05f) dt = 0.05f
        elapsed += dt
        update(dt)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun update(dt: Float) {
        if (particles.size > 1600) particles.subList(0, particles.size - 1600).clear()

        var i = rockets.size - 1
        while (i >= 0) {
            val r = rockets[i]
            r.vy += 300f * dt
            r.x += r.vx * dt
            r.y += r.vy * dt
            if (Random.nextFloat() < 0.9f) {
                particles.add(
                    Particle(
                        r.x, r.y,
                        Random.nextFloat() * 40 - 20, Random.nextFloat() * 30,
                        0.5f, 0.5f,
                        Color.rgb(255, 226, 150), Random.nextFloat() * 2 + 1.5f,
                        0f, false
                    )
                )
            }
            if (r.vy >= 0f || r.y <= r.targetY) {
                explode(r.x, r.y, r.hue)
                rockets.removeAt(i)
            }
            i--
        }

        i = particles.size - 1
        while (i >= 0) {
            val p = particles[i]
            p.vy += p.gravity * dt
            p.vx *= (1f - 0.6f * dt)
            p.vy *= (1f - 0.6f * dt)
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            if (p.life <= 0f) particles.removeAt(i)
            i--
        }
    }

    private fun explode(x: Float, y: Float, hue: Float) {
        val n = Random.nextInt(70, 130)
        val baseSpeed = Random.nextFloat() * 130 + 220
        repeat(n) {
            val ang = Random.nextFloat() * 6.2832f
            val sp = baseSpeed * (0.3f + Random.nextFloat() * 0.7f)
            val hsv = floatArrayOf(
                (hue + Random.nextFloat() * 40f - 20f + 360f) % 360f,
                0.7f + Random.nextFloat() * 0.3f,
                1f
            )
            particles.add(
                Particle(
                    x, y,
                    cos(ang) * sp, sin(ang) * sp,
                    Random.nextFloat() * 0.9f + 0.9f, 1.8f,
                    Color.HSVToColor(hsv),
                    Random.nextFloat() * 3 + 2.5f,
                    220f,
                    Random.nextFloat() < 0.35f
                )
            )
        }
    }

    private fun launchRocket(x: Float, targetY: Float) {
        val h = height.toFloat()
        val scale = (h / 2000f).coerceAtLeast(0.6f)
        rockets.add(
            Rocket(
                x, h + 8f,
                Random.nextFloat() * 60 - 30,
                -(Random.nextFloat() * 160 + 900) * scale,
                targetY.coerceAtLeast(h * 0.12f),
                Random.nextFloat() * 360f
            )
        )
    }

    private fun sparkle(x: Float, y: Float) {
        repeat(3) {
            val hsv = floatArrayOf(Random.nextFloat() * 360f, 0.6f, 1f)
            particles.add(
                Particle(
                    x, y,
                    Random.nextFloat() * 120 - 60, Random.nextFloat() * 120 - 60,
                    Random.nextFloat() * 0.6f + 0.4f, 1f,
                    Color.HSVToColor(hsv), Random.nextFloat() * 3 + 2f,
                    120f, true
                )
            )
        }
    }

    private fun finale() {
        val w = width.toFloat()
        repeat(Random.nextInt(5, 9)) {
            launchRocket(Random.nextFloat() * w, height * (0.12f + Random.nextFloat() * 0.35f))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interacted) { interacted = true; interactAt = elapsed }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                launchRocket(event.getX(idx), event.getY(idx))
            }
            MotionEvent.ACTION_MOVE -> {
                for (p in 0 until event.pointerCount) sparkle(event.getX(p), event.getY(p))
            }
        }
        return true
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val gx = event.values[0] / SensorManager.GRAVITY_EARTH
        val gy = event.values[1] / SensorManager.GRAVITY_EARTH
        val gz = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gx * gx + gy * gy + gz * gz)
        if (gForce > 2.3f && elapsed - lastShake > 0.8f) {
            lastShake = elapsed
            if (!interacted) { interacted = true; interactAt = elapsed }
            finale()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        drawStarsAndMoon(canvas)

        val bc = bufferCanvas
        val buf = buffer
        if (bc != null && buf != null) {
            bc.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
            for (p in particles) {
                var alphaF = (p.life / p.maxLife).coerceIn(0f, 1f)
                if (p.twinkle) alphaF *= (0.45f + 0.55f * sin(elapsed * 28f + p.x))
                val a = (255 * alphaF).toInt().coerceIn(0, 255)
                glowPaint.color = p.color
                glowPaint.alpha = (a * 0.35f).toInt().coerceIn(0, 255)
                bc.drawCircle(p.x, p.y, p.size * 2.4f, glowPaint)
                paint.color = p.color
                paint.alpha = a
                bc.drawCircle(p.x, p.y, p.size, paint)
            }
            canvas.drawBitmap(buf, 0f, 0f, null)
        }

        drawHint(canvas)
    }

    private fun drawStarsAndMoon(canvas: Canvas) {
        for (s in stars) {
            val tw = 0.5f + 0.5f * sin(elapsed * 2.5f + s.phase)
            starPaint.alpha = (s.baseA * tw).toInt().coerceIn(0, 255)
            canvas.drawCircle(s.x, s.y, s.r, starPaint)
        }
        canvas.drawCircle(moonX, moonY, moonR * 3.4f, moonGlowPaint)
        moonPaint.color = Color.rgb(236, 240, 222)
        canvas.drawCircle(moonX, moonY, moonR, moonPaint)
        moonPaint.color = Color.argb(36, 0, 0, 0)
        canvas.drawCircle(moonX + moonR * 0.35f, moonY - moonR * 0.2f, moonR * 0.2f, moonPaint)
        canvas.drawCircle(moonX - moonR * 0.25f, moonY + moonR * 0.28f, moonR * 0.13f, moonPaint)
    }

    private fun drawHint(canvas: Canvas) {
        val a = hintAlpha()
        if (a <= 0f) return
        val cx = width / 2f
        val cy = height * 0.46f
        textPaint.alpha = (255 * a).toInt()
        subTextPaint.alpha = (230 * a).toInt()
        canvas.drawText(hintLine1, cx, cy, textPaint)
        canvas.drawText(hintLine2, cx, cy + textPaint.textSize * 1.5f, subTextPaint)
    }

    private fun hintAlpha(): Float = when {
        interacted -> (1f - (elapsed - interactAt) / 1.2f).coerceIn(0f, 1f)
        elapsed < 4f -> 1f
        else -> (1f - (elapsed - 4f) / 2.5f).coerceIn(0f, 1f)
    }
}
