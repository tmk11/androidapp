package com.example.simpleapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Transparent overlay drawn on top of the camera preview. It maps the detected
 * faces from image coordinates into view coordinates (handling front-camera
 * mirroring and center-crop scaling) and paints the selected cute filter.
 */
class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    @Volatile private var faces: List<Face> = emptyList()
    private var imageW = 1
    private var imageH = 1
    private var front = true
    private var filterIndex = 0

    private val filterNames = listOf("Mèo 🐱", "Nón sinh nhật 🎉", "Kính 🕶️", "Cún con 🐶")

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val path = Path()

    fun update(faces: List<Face>, imageWidth: Int, imageHeight: Int, front: Boolean) {
        this.faces = faces
        this.imageW = imageWidth.coerceAtLeast(1)
        this.imageH = imageHeight.coerceAtLeast(1)
        this.front = front
        postInvalidate()
    }

    fun setFrontCamera(f: Boolean) { front = f }

    fun nextFilter() {
        filterIndex = (filterIndex + 1) % filterNames.size
        postInvalidate()
    }

    fun currentFilterName(): String = filterNames[filterIndex]

    private fun mapX(x: Float, scale: Float, dx: Float): Float {
        val vx = x * scale + dx
        return if (front) width - vx else vx
    }

    private fun mapY(y: Float, scale: Float, dy: Float): Float = y * scale + dy

    private fun landmark(face: Face, type: Int, scale: Float, dx: Float, dy: Float): PointF? {
        val p = face.getLandmark(type)?.position ?: return null
        return PointF(mapX(p.x, scale, dx), mapY(p.y, scale, dy))
    }

    override fun onDraw(canvas: Canvas) {
        val fs = faces
        if (fs.isEmpty() || width == 0 || height == 0) return

        val scale = max(width.toFloat() / imageW, height.toFloat() / imageH)
        val dx = (width - imageW * scale) / 2f
        val dy = (height - imageH * scale) / 2f

        for (face in fs) {
            val box = face.boundingBox
            val a = mapX(box.left.toFloat(), scale, dx)
            val b = mapX(box.right.toFloat(), scale, dx)
            val left = min(a, b)
            val right = max(a, b)
            val top = mapY(box.top.toFloat(), scale, dy)
            val bottom = mapY(box.bottom.toFloat(), scale, dy)
            val w = right - left
            val h = bottom - top
            if (w <= 0f || h <= 0f) continue
            val cx = (left + right) / 2f

            val leftEye = landmark(face, FaceLandmark.LEFT_EYE, scale, dx, dy)
                ?: PointF(left + w * 0.32f, top + h * 0.42f)
            val rightEye = landmark(face, FaceLandmark.RIGHT_EYE, scale, dx, dy)
                ?: PointF(right - w * 0.32f, top + h * 0.42f)
            val nose = landmark(face, FaceLandmark.NOSE_BASE, scale, dx, dy)
                ?: PointF(cx, top + h * 0.55f)

            when (filterIndex) {
                0 -> drawCat(canvas, left, right, top, w, h, nose)
                1 -> drawPartyHat(canvas, top, w, h, cx)
                2 -> drawGlasses(canvas, leftEye, rightEye)
                3 -> drawPuppy(canvas, left, right, top, w, h, cx, nose)
            }
        }
    }

    private fun drawCat(
        canvas: Canvas, left: Float, right: Float, top: Float,
        w: Float, h: Float, nose: PointF
    ) {
        val earW = w * 0.36f
        val earH = h * 0.45f
        // left ear (outer + inner)
        triangle(
            canvas,
            left + w * 0.04f, top + h * 0.12f,
            left + w * 0.04f + earW, top + h * 0.02f,
            left + w * 0.02f + earW * 0.5f, top - earH * 0.6f,
            Color.rgb(120, 120, 130)
        )
        triangle(
            canvas,
            left + w * 0.10f, top + h * 0.07f,
            left + w * 0.04f + earW * 0.78f, top,
            left + w * 0.05f + earW * 0.45f, top - earH * 0.38f,
            Color.rgb(255, 183, 197)
        )
        // right ear (outer + inner)
        triangle(
            canvas,
            right - w * 0.04f, top + h * 0.12f,
            right - w * 0.04f - earW, top + h * 0.02f,
            right - w * 0.02f - earW * 0.5f, top - earH * 0.6f,
            Color.rgb(120, 120, 130)
        )
        triangle(
            canvas,
            right - w * 0.10f, top + h * 0.07f,
            right - w * 0.04f - earW * 0.78f, top,
            right - w * 0.05f - earW * 0.45f, top - earH * 0.38f,
            Color.rgb(255, 183, 197)
        )
        // nose
        triangle(
            canvas,
            nose.x - w * 0.06f, nose.y,
            nose.x + w * 0.06f, nose.y,
            nose.x, nose.y + h * 0.05f,
            Color.rgb(255, 111, 145)
        )
        // whiskers
        stroke.color = Color.WHITE
        stroke.strokeWidth = max(2f, w * 0.012f)
        for (k in -1..1) {
            val yy = nose.y + k * h * 0.04f
            canvas.drawLine(nose.x - w * 0.10f, nose.y, nose.x - w * 0.42f, yy - h * 0.02f, stroke)
            canvas.drawLine(nose.x + w * 0.10f, nose.y, nose.x + w * 0.42f, yy - h * 0.02f, stroke)
        }
    }

    private fun drawPartyHat(canvas: Canvas, top: Float, w: Float, h: Float, cx: Float) {
        val hatH = h * 0.85f
        val baseW = w * 0.72f
        val baseY = top + h * 0.06f
        val apexY = top - hatH
        // body
        path.reset()
        path.moveTo(cx - baseW / 2f, baseY)
        path.lineTo(cx + baseW / 2f, baseY)
        path.lineTo(cx, apexY)
        path.close()
        fill.color = Color.rgb(255, 95, 162)
        canvas.drawPath(path, fill)
        // stripes
        stroke.color = Color.rgb(255, 213, 79)
        stroke.strokeWidth = max(4f, w * 0.03f)
        var t = 0.25f
        while (t < 1f) {
            val y = baseY + (apexY - baseY) * t
            val halfW = (baseW / 2f) * (1f - t)
            canvas.drawLine(cx - halfW, y, cx + halfW, y, stroke)
            t += 0.25f
        }
        // pompom
        fill.color = Color.rgb(255, 213, 79)
        canvas.drawCircle(cx, apexY, w * 0.07f, fill)
    }

    private fun drawGlasses(canvas: Canvas, leftEye: PointF, rightEye: PointF) {
        val d = hypot(rightEye.x - leftEye.x, rightEye.y - leftEye.y)
        val r = max(d * 0.36f, 24f)
        // lens fill
        fill.color = Color.argb(70, 10, 10, 20)
        canvas.drawCircle(leftEye.x, leftEye.y, r, fill)
        canvas.drawCircle(rightEye.x, rightEye.y, r, fill)
        // frames
        stroke.color = Color.rgb(20, 20, 30)
        stroke.strokeWidth = max(5f, d * 0.05f)
        canvas.drawCircle(leftEye.x, leftEye.y, r, stroke)
        canvas.drawCircle(rightEye.x, rightEye.y, r, stroke)
        // bridge + arms
        canvas.drawLine(leftEye.x + r, leftEye.y, rightEye.x - r, rightEye.y, stroke)
        canvas.drawLine(leftEye.x - r, leftEye.y, leftEye.x - r - d * 0.35f, leftEye.y - d * 0.1f, stroke)
        canvas.drawLine(rightEye.x + r, rightEye.y, rightEye.x + r + d * 0.35f, rightEye.y - d * 0.1f, stroke)
        // sparkles
        fill.color = Color.argb(160, 255, 255, 255)
        canvas.drawCircle(leftEye.x - r * 0.4f, leftEye.y - r * 0.4f, r * 0.16f, fill)
        canvas.drawCircle(rightEye.x - r * 0.4f, rightEye.y - r * 0.4f, r * 0.16f, fill)
    }

    private fun drawPuppy(
        canvas: Canvas, left: Float, right: Float, top: Float,
        w: Float, h: Float, cx: Float, nose: PointF
    ) {
        val brown = Color.rgb(141, 110, 99)
        val darkBrown = Color.rgb(93, 64, 55)
        // floppy ears
        fill.color = brown
        canvas.drawOval(RectF(left - w * 0.16f, top + h * 0.02f, left + w * 0.20f, top + h * 0.78f), fill)
        canvas.drawOval(RectF(right - w * 0.20f, top + h * 0.02f, right + w * 0.16f, top + h * 0.78f), fill)
        fill.color = darkBrown
        canvas.drawOval(RectF(left - w * 0.10f, top + h * 0.08f, left + w * 0.16f, top + h * 0.66f), fill)
        canvas.drawOval(RectF(right - w * 0.16f, top + h * 0.08f, right + w * 0.10f, top + h * 0.66f), fill)
        // nose
        fill.color = Color.rgb(30, 30, 30)
        canvas.drawOval(RectF(nose.x - w * 0.11f, nose.y - h * 0.02f, nose.x + w * 0.11f, nose.y + h * 0.10f), fill)
        fill.color = Color.argb(150, 255, 255, 255)
        canvas.drawCircle(nose.x - w * 0.03f, nose.y + h * 0.005f, w * 0.025f, fill)
        // tongue
        fill.color = Color.rgb(255, 138, 149)
        val ty = nose.y + h * 0.16f
        canvas.drawRoundRect(RectF(cx - w * 0.10f, ty, cx + w * 0.10f, ty + h * 0.22f), w * 0.10f, w * 0.10f, fill)
        stroke.color = Color.rgb(214, 90, 110)
        stroke.strokeWidth = max(2f, w * 0.012f)
        canvas.drawLine(cx, ty + h * 0.03f, cx, ty + h * 0.20f, stroke)
    }

    private fun triangle(
        canvas: Canvas,
        x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int
    ) {
        path.reset()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.close()
        fill.color = color
        canvas.drawPath(path, fill)
    }
}
