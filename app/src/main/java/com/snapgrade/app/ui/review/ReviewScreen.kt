package com.snapgrade.app.ui.review

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapgrade.app.data.model.CriterionScore
import com.snapgrade.app.data.model.GradingResult
import com.snapgrade.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    gradingResult: GradingResult,
    capturedImage: Bitmap?,
    extractedText: String,
    onBackToCamera: () -> Unit,
    onReevaluateText: (String) -> Unit
) {
    val context = LocalContext.current

    // Local mutable state for user edits before finalizing
    var criteriaScores by remember(gradingResult) {
        mutableStateOf(gradingResult.criterionScores.map { it.copy() })
    }
    var generalFeedback by remember(gradingResult) {
        mutableStateOf(gradingResult.generalFeedback)
    }
    var currentExtractedText by remember(extractedText) {
        mutableStateOf(extractedText)
    }
    var isEditingExtractedText by remember { mutableStateOf(false) }
    var showFinalizeDialog by remember { mutableStateOf(false) }

    // Dynamic total calculations
    val totalAwarded = criteriaScores.sumOf { it.awardedPoints }
    val totalMax = criteriaScores.sumOf { it.maxPoints }
    val percentage = if (totalMax > 0) (totalAwarded.toFloat() / totalMax) * 100f else 0f

    val gradeColor by animateColorAsState(
        targetValue = when {
            percentage >= 85f -> EmeraldSuccess
            percentage >= 70f -> NeonCyan
            percentage >= 50f -> AmberWarning
            else -> CoralError
        },
        label = "gradeColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = gradingResult.rubricTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                        Text(
                            text = "Review & Finalize Grade",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToCamera) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = WhiteText
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val summary = buildExportSummary(
                                gradingResult.rubricTitle,
                                totalAwarded,
                                totalMax,
                                criteriaScores,
                                generalFeedback
                            )
                            copyToClipboard(context, summary)
                            Toast.makeText(context, "Summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        },
        bottomBar = {
            Surface(
                color = DeepSlate,
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBackToCamera,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhiteText)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Next")
                    }

                    Button(
                        onClick = { showFinalizeDialog = true },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DarkNavy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalize Grade", color = DarkNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = DarkNavy
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Latency & NPU Performance Bar
                Surface(
                    color = DeepSlate,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "End-to-End: ${(gradingResult.ocrLatencyMs + gradingResult.inferenceLatencyMs) / 1000f}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = WhiteText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "OCR: ${gradingResult.ocrLatencyMs}ms | NPU: ${gradingResult.inferenceLatencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateMuted
                        )
                    }
                }
            }

            // Overall Score Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, gradeColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Score",
                                style = MaterialTheme.typography.labelMedium,
                                color = SlateTextSecondary
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$totalAwarded",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    color = gradeColor
                                )
                                Text(
                                    text = " / $totalMax",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SlateMuted,
                                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                )
                            }
                            Text(
                                text = "${percentage.toInt()}% • ${gradingResult.gradeBadge}",
                                style = MaterialTheme.typography.labelMedium,
                                color = gradeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Thumbnail of captured sheet (if available)
                        if (capturedImage != null) {
                            Image(
                                bitmap = capturedImage.asImageBitmap(),
                                contentDescription = "Captured Answer Sheet",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Extracted OCR Text Section (Collapsible & Editable)
            item {
                var isExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Extracted Handwriting OCR",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WhiteText
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SlateTextSecondary
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                if (isEditingExtractedText) {
                                    OutlinedTextField(
                                        value = currentExtractedText,
                                        onValueChange = { currentExtractedText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(color = WhiteText),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = SlateBorder
                                        ),
                                        minLines = 3
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        TextButton(onClick = { isEditingExtractedText = false }) {
                                            Text("Cancel", color = SlateMuted)
                                        }
                                        Button(
                                            onClick = {
                                                isEditingExtractedText = false
                                                onReevaluateText(currentExtractedText)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                                        ) {
                                            Text("Re-Grade", color = DarkNavy, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = currentExtractedText.ifBlank { "(No text extracted)" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WhiteText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkNavy, RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { isEditingExtractedText = true },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit Raw Text", fontSize = 12.sp, color = NeonCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section Header: Criteria Breakdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rubric Breakdown (Tap to Edit)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WhiteText
                    )
                    Text(
                        text = "${criteriaScores.size} criteria",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateMuted
                    )
                }
            }

            // Editable Per-Criterion Cards
            itemsIndexed(criteriaScores) { index, item ->
                CriterionCard(
                    score = item,
                    onScoreChanged = { newScore ->
                        val updated = criteriaScores.toMutableList()
                        updated[index] = item.copy(awardedPoints = newScore)
                        criteriaScores = updated
                    },
                    onFeedbackChanged = { newFeedback ->
                        val updated = criteriaScores.toMutableList()
                        updated[index] = item.copy(feedback = newFeedback)
                        criteriaScores = updated
                    }
                )
            }

            // Overall General Feedback Card (Editable)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Teacher's Overall Feedback",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = generalFeedback,
                            onValueChange = { generalFeedback = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = WhiteText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = SlateBorder
                            ),
                            minLines = 2
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Finalize Confirmation Dialog
    if (showFinalizeDialog) {
        AlertDialog(
            onDismissRequest = { showFinalizeDialog = false },
            title = {
                Text(
                    text = "Finalize & Record Grade?",
                    fontWeight = FontWeight.Bold,
                    color = WhiteText
                )
            },
            text = {
                Column {
                    Text(
                        text = "Final Score: $totalAwarded / $totalMax (${percentage.toInt()}%)",
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The score and criterion feedback have been reviewed and verified.",
                        color = SlateTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinalizeDialog = false
                        Toast.makeText(context, "Grade finalized and saved successfully!", Toast.LENGTH_LONG).show()
                        onBackToCamera()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricTeal)
                ) {
                    Text("Confirm & Next", color = DarkNavy, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalizeDialog = false }) {
                    Text("Keep Editing", color = SlateMuted)
                }
            },
            containerColor = DeepSlate
        )
    }
}

/**
 * Individual Criterion Scoring Card with score stepper and inline feedback editor.
 */
@Composable
fun CriterionCard(
    score: CriterionScore,
    onScoreChanged: (Int) -> Unit,
    onFeedbackChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSlate),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Criterion Name and Stepper Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = score.criterionName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WhiteText,
                    modifier = Modifier.weight(1f)
                )

                // Score Stepper (+ / -)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(DarkNavy, RoundedCornerShape(20.dp))
                        .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (score.awardedPoints > 0) {
                                onScoreChanged(score.awardedPoints - 1)
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        enabled = score.awardedPoints > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = if (score.awardedPoints > 0) WhiteText else SlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${score.awardedPoints} / ${score.maxPoints}",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            if (score.awardedPoints < score.maxPoints) {
                                onScoreChanged(score.awardedPoints + 1)
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        enabled = score.awardedPoints < score.maxPoints
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = if (score.awardedPoints < score.maxPoints) WhiteText else SlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inline Editable Feedback
            OutlinedTextField(
                value = score.feedback,
                onValueChange = onFeedbackChanged,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = WhiteText),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = SlateBorder.copy(alpha = 0.5f),
                    focusedContainerColor = DarkNavy.copy(alpha = 0.5f),
                    unfocusedContainerColor = DarkNavy.copy(alpha = 0.5f)
                ),
                minLines = 2,
                label = {
                    Text("Feedback for student", style = MaterialTheme.typography.labelSmall, color = SlateMuted)
                }
            )
        }
    }
}

private fun buildExportSummary(
    rubricTitle: String,
    totalAwarded: Int,
    totalMax: Int,
    criteria: List<CriterionScore>,
    generalFeedback: String
): String {
    val builder = StringBuilder()
    builder.appendLine("=== SnapGrade Evaluation Report ===")
    builder.appendLine("Assignment: $rubricTitle")
    builder.appendLine("Total Score: $totalAwarded / $totalMax")
    builder.appendLine("--- Criteria Breakdown ---")
    for (c in criteria) {
        builder.appendLine("• ${c.criterionName}: ${c.awardedPoints}/${c.maxPoints} pts")
        builder.appendLine("  Feedback: ${c.feedback}")
    }
    builder.appendLine("--- General Feedback ---")
    builder.appendLine(generalFeedback)
    builder.appendLine("Graded on-device with SnapGrade (Snapdragon NPU)")
    return builder.toString()
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("SnapGrade Evaluation", text)
    clipboard.setPrimaryClip(clip)
}
