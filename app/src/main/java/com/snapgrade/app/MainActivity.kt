package com.snapgrade.app

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.snapgrade.app.ai.GradingEngine
import com.snapgrade.app.ai.OcrManager
import com.snapgrade.app.data.model.DemoRubrics
import com.snapgrade.app.data.model.GradingResult
import com.snapgrade.app.data.model.Rubric
import com.snapgrade.app.data.model.SampleAnswer
import com.snapgrade.app.ui.camera.CameraCaptureScreen
import com.snapgrade.app.ui.review.ReviewScreen
import com.snapgrade.app.ui.theme.DarkNavy
import com.snapgrade.app.ui.theme.SnapGradeTheme
import kotlinx.coroutines.launch

enum class AppScreen {
    CAMERA,
    REVIEW
}

class MainActivity : ComponentActivity() {

    private lateinit var ocrManager: OcrManager
    private lateinit var gradingEngine: GradingEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ocrManager = OcrManager()
        gradingEngine = GradingEngine(applicationContext)

        setContent {
            SnapGradeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavy
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.CAMERA) }
                    var currentRubric by remember { mutableStateOf<Rubric>(DemoRubrics.PHOTOSYNTHESIS) }
                    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
                    var extractedText by remember { mutableStateOf("") }
                    var gradingResult by remember { mutableStateOf<GradingResult?>(null) }
                    var isProcessing by remember { mutableStateOf(false) }
                    var processingStatus by remember { mutableStateOf("") }

                    when (currentScreen) {
                        AppScreen.CAMERA -> {
                            CameraCaptureScreen(
                                currentRubric = currentRubric,
                                onRubricSelected = { newRubric ->
                                    currentRubric = newRubric
                                },
                                onImageCaptured = { bitmap, rotation ->
                                    capturedImage = bitmap
                                    isProcessing = true
                                    processingStatus = "🔍 ML Kit Handwriting OCR..."

                                    lifecycleScope.launch {
                                        val ocrResult = ocrManager.recognizeText(bitmap, rotation)
                                        if (ocrResult.isSuccess && ocrResult.text.isNotBlank()) {
                                            extractedText = ocrResult.text
                                            processingStatus = "🧠 Snapdragon NPU Grading..."
                                            val result = gradingEngine.gradeAnswer(
                                                studentText = ocrResult.text,
                                                rubric = currentRubric,
                                                ocrLatency = ocrResult.latencyMs
                                            )
                                            gradingResult = result
                                            isProcessing = false
                                            currentScreen = AppScreen.REVIEW
                                        } else {
                                            isProcessing = false
                                            Toast.makeText(
                                                this@MainActivity,
                                                "No clear handwriting detected. Please try holding closer or steadying the camera.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                onSampleSelected = { sample: SampleAnswer, rubric: Rubric ->
                                    currentRubric = rubric
                                    capturedImage = null
                                    extractedText = sample.text
                                    isProcessing = true
                                    processingStatus = "🧠 Snapdragon NPU Grading..."

                                    lifecycleScope.launch {
                                        val result = gradingEngine.gradeAnswer(
                                            studentText = sample.text,
                                            rubric = rubric,
                                            ocrLatency = 135L // Simulated fast on-device OCR latency
                                        )
                                        gradingResult = result
                                        isProcessing = false
                                        currentScreen = AppScreen.REVIEW
                                    }
                                },
                                isProcessing = isProcessing,
                                processingStatus = processingStatus
                            )
                        }

                        AppScreen.REVIEW -> {
                            gradingResult?.let { result ->
                                ReviewScreen(
                                    gradingResult = result,
                                    capturedImage = capturedImage,
                                    extractedText = extractedText,
                                    onBackToCamera = {
                                        currentScreen = AppScreen.CAMERA
                                    },
                                    onReevaluateText = { updatedText ->
                                        extractedText = updatedText
                                        lifecycleScope.launch {
                                            val reResult = gradingEngine.gradeAnswer(
                                                studentText = updatedText,
                                                rubric = currentRubric,
                                                ocrLatency = result.ocrLatencyMs
                                            )
                                            gradingResult = reResult
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrManager.close()
    }
}
