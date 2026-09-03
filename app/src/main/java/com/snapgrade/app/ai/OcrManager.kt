package com.snapgrade.app.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.system.measureTimeMillis

data class OcrResult(
    val text: String,
    val latencyMs: Long,
    val blockCount: Int,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

/**
 * On-Device OCR Manager using Google ML Kit Text Recognition v2.
 * Hardware-accelerated and runs 100% offline on Snapdragon devices.
 */
class OcrManager {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extracts handwritten/printed text from a Bitmap on-device.
     */
    suspend fun recognizeText(bitmap: Bitmap, rotationDegrees: Int = 0): OcrResult {
        var recognizedText = ""
        var blocks = 0
        var isSuccess = false
        var errorMsg: String? = null

        val latency = measureTimeMillis {
            try {
                // Preprocess for handwriting contrast enhancement
                val processedBitmap = enhanceHandwritingContrast(bitmap)
                val inputImage = InputImage.fromBitmap(processedBitmap, rotationDegrees)

                val textVision: Text = suspendCancellableCoroutine { continuation ->
                    val task: Task<Text> = recognizer.process(inputImage)
                    task.addOnSuccessListener { result ->
                        continuation.resume(result)
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "ML Kit OCR failed: ${exception.message}", exception)
                        continuation.resumeWith(Result.failure(exception))
                    }
                }

                recognizedText = cleanExtractedText(textVision.text)
                blocks = textVision.textBlocks.size
                isSuccess = true
            } catch (e: Exception) {
                Log.e(TAG, "OCR recognition error", e)
                errorMsg = e.localizedMessage ?: "OCR extraction error"
            }
        }

        Log.d(TAG, "OCR completed in ${latency}ms, blocks: $blocks, length: ${recognizedText.length}")
        return OcrResult(
            text = recognizedText,
            latencyMs = latency,
            blockCount = blocks,
            isSuccess = isSuccess,
            errorMessage = errorMsg
        )
    }

    /**
     * Normalizes line breaks and cleans stray noise characters from handwriting OCR.
     */
    private fun cleanExtractedText(raw: String): String {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    /**
     * Increases contrast and normalizes greyscale to assist reading faint pencil/pen handwriting.
     */
    private fun enhanceHandwritingContrast(src: Bitmap): Bitmap {
        return try {
            val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint()

            // 1.35x contrast boost, slight brightness adjustment
            val cm = ColorMatrix(
                floatArrayOf(
                    1.35f, 0f, 0f, 0f, -25f,
                    0f, 1.35f, 0f, 0f, -25f,
                    0f, 0f, 1.35f, 0f, -25f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(src, 0f, 0f, paint)
            output
        } catch (e: Exception) {
            // Fallback to source bitmap if allocation fails
            src
        }
    }

    fun close() {
        recognizer.close()
    }

    companion object {
        private const val TAG = "SnapGrade:OcrManager"
    }
}
