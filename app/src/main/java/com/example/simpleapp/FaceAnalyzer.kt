package com.example.simpleapp

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector

/**
 * Feeds CameraX frames into ML Kit's face detector and forwards the detected
 * faces (plus the rotated image size) to the overlay for drawing.
 */
class FaceAnalyzer(
    private val detector: FaceDetector,
    private val overlay: FaceOverlayView,
    private val isFront: () -> Boolean
) : ImageAnalysis.Analyzer {

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        // ML Kit returns coordinates in the upright (rotated) frame.
        val w: Int
        val h: Int
        if (rotation == 90 || rotation == 270) {
            w = imageProxy.height
            h = imageProxy.width
        } else {
            w = imageProxy.width
            h = imageProxy.height
        }

        detector.process(image)
            .addOnSuccessListener { faces -> overlay.update(faces, w, h, isFront()) }
            .addOnCompleteListener { imageProxy.close() }
    }
}
