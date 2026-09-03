package com.snapgrade.app.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.snapgrade.app.data.model.DemoRubrics
import com.snapgrade.app.data.model.Rubric
import com.snapgrade.app.data.model.SampleAnswer
import com.snapgrade.app.ui.theme.*
import java.io.InputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    currentRubric: Rubric,
    onRubricSelected: (Rubric) -> Unit,
    onImageCaptured: (Bitmap, Int) -> Unit,
    onSampleSelected: (SampleAnswer, Rubric) -> Unit,
    isProcessing: Boolean,
    processingStatus: String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Photo picker for selecting from gallery/storage
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(context, it)
            if (bitmap != null) {
                onImageCaptured(bitmap, 0)
            }
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var showRubricSheet by remember { mutableStateOf(false) }
    var showSampleSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        if (hasCameraPermission) {
            // CameraX Live Viewfinder
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Reticle overlay
            DocumentReticleOverlay()
        } else {
            // Fallback screen when camera permission is denied
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = ElectricTeal,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = WhiteText,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SnapGrade requires camera access to scan handwritten assignments and answer sheets on-device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Grant Permission", color = DarkNavy, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Control Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rubric Selection Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showRubricSheet = true },
                    color = DeepSlate.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentRubric.title,
                                color = WhiteText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentRubric.subject} • ${currentRubric.totalMaxPoints} pts",
                                color = SlateTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                    }
                }

                // Flash / Torch Toggle
                IconButton(
                    onClick = {
                        val newTorch = !isTorchOn
                        isTorchOn = newTorch
                        camera?.cameraControl?.enableTorch(newTorch)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(DeepSlate.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) AmberWarning else WhiteText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Snapdragon NPU Badge
            Surface(
                color = ElectricTeal.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.3f)),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% On-Device AI • Snapdragon NPU",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Demo Samples Button (Allows instant testing with realistic student handwriting)
            TextButton(
                onClick = { showSampleSheet = true },
                colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Demo Pre-Loaded Student Submissions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery / File Picker
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(DeepSlate.copy(alpha = 0.9f), CircleShape)
                        .border(1.dp, SlateBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pick Image",
                        tint = WhiteText
                    )
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color.Transparent, CircleShape)
                        .border(4.dp, NeonCyan, CircleShape)
                        .clickable(enabled = !isProcessing) {
                            val capture = imageCapture ?: return@clickable
                            capturePhoto(context, capture) { bitmap, rotation ->
                                onImageCaptured(bitmap, rotation)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(WhiteText, CircleShape)
                    )
                }

                // Quick Rubric Details Button
                IconButton(
                    onClick = { showRubricSheet = true },
                    modifier = Modifier
                        .size(52.dp)
                        .background(DeepSlate.copy(alpha = 0.9f), CircleShape)
                        .border(1.dp, SlateBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = "View Rubric",
                        tint = WhiteText
                    )
                }
            }
        }

        // Processing Overlay with Live Steps
        AnimatedVisibility(
            visible = isProcessing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkNavy.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = processingStatus,
                            color = WhiteText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Target Latency: < 5s • Snapdragon NPU",
                            color = SlateMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet: Rubric Selector
    if (showRubricSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRubricSheet = false },
            containerColor = DeepSlate,
            contentColor = WhiteText
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select Assignment Rubric",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText
                )
                Text(
                    text = "The on-device grading model will score student answers against this rubric.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(DemoRubrics.ALL) { rubric ->
                        val isSelected = rubric.id == currentRubric.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRubricSelected(rubric)
                                    showRubricSheet = false
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SlateBorder else DarkNavy
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) NeonCyan else SlateBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rubric.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeonCyan else WhiteText
                                    )
                                    Surface(
                                        color = ElectricTeal.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${rubric.totalMaxPoints} pts",
                                            color = NeonCyan,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = rubric.subject,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = rubric.assignmentPrompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${rubric.criteria.size} Criteria: ${rubric.criteria.joinToString { it.name }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricTeal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet: Demo Sample Answers
    if (showSampleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSampleSheet = false },
            containerColor = DeepSlate,
            contentColor = WhiteText
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Demo Pre-Loaded Student Submissions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText
                )
                Text(
                    text = "Select a simulated handwritten answer to test instant on-device grading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(currentRubric.sampleAnswers) { sample ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSampleSheet = false
                                    onSampleSelected(sample, currentRubric)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sample.studentName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sample.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\"${sample.text}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WhiteText,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws the document alignment frame overlay on the camera viewfinder.
 */
@Composable
fun DocumentReticleOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxWidth = width * 0.88f
        val boxHeight = height * 0.62f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2.3f

        // Darkened surrounding mask
        drawRect(
            color = Color(0x66000000),
            size = size
        )

        // Clear hole for document viewport
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // Teal border outline
        drawRoundRect(
            color = ElectricTeal.copy(alpha = 0.85f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Bitmap, Int) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val bitmap = imageProxyToBitmap(imageProxy)
                imageProxy.close()
                if (bitmap != null) {
                    onCaptured(bitmap, rotationDegrees)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val rotation = image.imageInfo.rotationDegrees
    return if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
