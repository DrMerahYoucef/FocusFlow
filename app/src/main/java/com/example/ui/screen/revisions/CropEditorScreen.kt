package com.example.ui.screen.revisions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.ExifInterface
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeumorphicColors
import com.example.data.repository.toSingleNote
import java.io.File

enum class CropMode { RECTANGLE, LASSO }
enum class HandleType { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

@Composable
fun CropEditorScreen(
    imagePath: String,
    viewModel: RevisionsViewModel,
    onCropConfirmed: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val primaryColor = NeumorphicColors.Primary

    val sourceFile = remember(imagePath) { File(imagePath) }
    val baseBitmap = remember(imagePath) {
        if (sourceFile.exists()) {
            loadOrientedBitmap(imagePath)
        } else null
    }
    var workingBitmap by remember(baseBitmap) { mutableStateOf(baseBitmap) }

    var cropMode by remember { mutableStateOf(CropMode.RECTANGLE) }
    var showModeDialog by remember { mutableStateOf(false) }
    var selectedExplainMode by remember { mutableStateOf(false) }
    var pendingCroppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessageToShow by remember { mutableStateOf<String?>(null) }

    // Rectangle crop state (normalized 0..1 scale relative to bitmap)
    var rectNormLeft by remember { mutableStateOf(0.1f) }
    var rectNormTop by remember { mutableStateOf(0.2f) }
    var rectNormRight by remember { mutableStateOf(0.9f) }
    var rectNormBottom by remember { mutableStateOf(0.8f) }

    // Lasso path points state (normalized 0..1 scale relative to bitmap)
    var lassoNormPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    if (workingBitmap == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Impossible de charger l'image", color = Color.White)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val containerWidthPx = constraints.maxWidth.toFloat()
            val containerHeightPx = constraints.maxHeight.toFloat()

            val bmp = workingBitmap!!
            val bmpW = bmp.width.toFloat()
            val bmpH = bmp.height.toFloat()

            // Calculate exact displayed size and letterbox offsets under ContentScale.Fit
            val scale = minOf(containerWidthPx / bmpW, containerHeightPx / bmpH)
            val displayedW = bmpW * scale
            val displayedH = bmpH * scale
            val offsetX = (containerWidthPx - displayedW) / 2f
            val offsetY = (containerHeightPx - displayedH) / 2f

            fun normToScreenX(nx: Float): Float = offsetX + nx * displayedW
            fun normToScreenY(ny: Float): Float = offsetY + ny * displayedH

            fun screenToNormX(sx: Float): Float = ((sx - offsetX) / displayedW).coerceIn(0f, 1f)
            fun screenToNormY(sy: Float): Float = ((sy - offsetY) / displayedH).coerceIn(0f, 1f)

            // Draw image scaled to fit
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Overlays based on mode
            if (cropMode == CropMode.RECTANGLE) {
                val currentLeft by rememberUpdatedState(rectNormLeft)
                val currentTop by rememberUpdatedState(rectNormTop)
                val currentRight by rememberUpdatedState(rectNormRight)
                val currentBottom by rememberUpdatedState(rectNormBottom)

                var draggedHandle by remember { mutableStateOf<HandleType?>(null) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val rLeft = normToScreenX(currentLeft)
                                    val rTop = normToScreenY(currentTop)
                                    val rRight = normToScreenX(currentRight)
                                    val rBottom = normToScreenY(currentBottom)
                                    val threshold = 70f

                                    draggedHandle = when {
                                        (offset - Offset(rLeft, rTop)).getDistance() < threshold -> HandleType.TOP_LEFT
                                        (offset - Offset(rRight, rTop)).getDistance() < threshold -> HandleType.TOP_RIGHT
                                        (offset - Offset(rLeft, rBottom)).getDistance() < threshold -> HandleType.BOTTOM_LEFT
                                        (offset - Offset(rRight, rBottom)).getDistance() < threshold -> HandleType.BOTTOM_RIGHT
                                        offset.x in rLeft..rRight && offset.y in rTop..rBottom -> HandleType.MOVE
                                        else -> null
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dx = dragAmount.x / displayedW
                                    val dy = dragAmount.y / displayedH

                                    when (draggedHandle) {
                                        HandleType.TOP_LEFT -> {
                                            rectNormLeft = (currentLeft + dx).coerceIn(0f, currentRight - 0.05f)
                                            rectNormTop = (currentTop + dy).coerceIn(0f, currentBottom - 0.05f)
                                        }
                                        HandleType.TOP_RIGHT -> {
                                            rectNormRight = (currentRight + dx).coerceIn(currentLeft + 0.05f, 1f)
                                            rectNormTop = (currentTop + dy).coerceIn(0f, currentBottom - 0.05f)
                                        }
                                        HandleType.BOTTOM_LEFT -> {
                                            rectNormLeft = (currentLeft + dx).coerceIn(0f, currentRight - 0.05f)
                                            rectNormBottom = (currentBottom + dy).coerceIn(currentTop + 0.05f, 1f)
                                        }
                                        HandleType.BOTTOM_RIGHT -> {
                                            rectNormRight = (currentRight + dx).coerceIn(currentLeft + 0.05f, 1f)
                                            rectNormBottom = (currentBottom + dy).coerceIn(currentTop + 0.05f, 1f)
                                        }
                                        HandleType.MOVE -> {
                                            val w = currentRight - currentLeft
                                            val h = currentBottom - currentTop
                                            val newL = (currentLeft + dx).coerceIn(0f, 1f - w)
                                            val newT = (currentTop + dy).coerceIn(0f, 1f - h)
                                            rectNormLeft = newL
                                            rectNormTop = newT
                                            rectNormRight = newL + w
                                            rectNormBottom = newT + h
                                        }
                                        null -> {}
                                    }
                                },
                                onDragEnd = { draggedHandle = null },
                                onDragCancel = { draggedHandle = null }
                            )
                        }
                ) {
                    val rLeft = normToScreenX(rectNormLeft)
                    val rTop = normToScreenY(rectNormTop)
                    val rRight = normToScreenX(rectNormRight)
                    val rBottom = normToScreenY(rectNormBottom)

                    val rect = Rect(rLeft, rTop, rRight, rBottom)

                    // Dim outer area
                    clipRect(rect.left, rect.top, rect.right, rect.bottom, clipOp = ClipOp.Difference) {
                        drawRect(Color.Black.copy(alpha = 0.6f))
                    }

                    // Draw bounding rectangle
                    drawRect(
                        color = Color.White,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = 4f)
                    )

                    // Corner handles
                    listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { handle ->
                        drawCircle(color = Color.White, radius = 18f, center = handle)
                        drawCircle(color = primaryColor, radius = 14f, center = handle)
                    }
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val nx = screenToNormX(offset.x)
                                    val ny = screenToNormY(offset.y)
                                    lassoNormPoints = listOf(Offset(nx, ny))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val nx = screenToNormX(change.position.x)
                                    val ny = screenToNormY(change.position.y)
                                    lassoNormPoints = lassoNormPoints + Offset(nx, ny)
                                }
                            )
                        }
                ) {
                    if (lassoNormPoints.size > 1) {
                        val path = Path().apply {
                            val first = lassoNormPoints.first()
                            moveTo(normToScreenX(first.x), normToScreenY(first.y))
                            for (i in 1 until lassoNormPoints.size) {
                                val pt = lassoNormPoints[i]
                                lineTo(normToScreenX(pt.x), normToScreenY(pt.y))
                            }
                        }
                        drawPath(path = path, color = Color(0xFF00E676), style = Stroke(width = 6f))
                    }
                }
            }
        }

        // Mode Selector Bar (Top)
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = cropMode == CropMode.RECTANGLE,
                        onClick = { cropMode = CropMode.RECTANGLE },
                        label = { Text("Rectangle") },
                        leadingIcon = { Icon(Icons.Default.CropLandscape, contentDescription = null) }
                    )
                    FilterChip(
                        selected = cropMode == CropMode.LASSO,
                        onClick = { cropMode = CropMode.LASSO },
                        label = { Text("Lasso") },
                        leadingIcon = { Icon(Icons.Default.Gesture, contentDescription = null) }
                    )
                    IconButton(
                        onClick = {
                            workingBitmap?.let { current ->
                                val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                                val rotated = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
                                workingBitmap = rotated
                                rectNormLeft = 0.1f
                                rectNormTop = 0.2f
                                rectNormRight = 0.9f
                                rectNormBottom = 0.8f
                                lassoNormPoints = emptyList()
                            }
                        },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90°", tint = Color.White)
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(20.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saisie manuelle", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        try {
                            val bitmapToCrop = workingBitmap ?: return@Button
                            val croppedBitmap = cropBitmap(
                                originalBitmap = bitmapToCrop,
                                mode = cropMode,
                                rectNormLeft = rectNormLeft,
                                rectNormTop = rectNormTop,
                                rectNormRight = rectNormRight,
                                rectNormBottom = rectNormBottom,
                                lassoNormPoints = lassoNormPoints
                            )

                            pendingCroppedBitmap = croppedBitmap
                            selectedExplainMode = false // Default to verbatim text
                            showModeDialog = true
                        } catch (e: Exception) {
                            Toast.makeText(context, "Crop error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Générer (Gemini)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Extraction Mode & Cropped Image Preview Dialog
        if (showModeDialog && pendingCroppedBitmap != null) {
            var selectedDeckId by remember { mutableStateOf(state.selectedDeckId) }
            var temporaryPromptAddendum by remember { mutableStateOf("") }
            var cardCreationType by remember { mutableStateOf("OCR") } // "OCR" or "LOCAL_IMAGE"
            var isDeckDropdownExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    showModeDialog = false
                    pendingCroppedBitmap = null
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropLandscape,
                            contentDescription = null,
                            tint = NeumorphicColors.Primary
                        )
                        Text(
                            text = "Card Creation & Options",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Display the cropped image
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeumorphicColors.Primary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 180.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = pendingCroppedBitmap!!.asImageBitmap(),
                                    contentDescription = "Cropped Image Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Target Deck Selector (Section 8)
                        Text("Target Deck:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val activeDeckName = state.decks.find { it.id == selectedDeckId }?.name ?: "Select Deck"
                            OutlinedButton(
                                onClick = { isDeckDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(activeDeckName, color = NeumorphicColors.TextPrimary, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Edit, contentDescription = null, tint = NeumorphicColors.Primary)
                            }
                            DropdownMenu(
                                expanded = isDeckDropdownExpanded,
                                onDismissRequest = { isDeckDropdownExpanded = false }
                            ) {
                                state.decks.forEach { deck ->
                                    DropdownMenuItem(
                                        text = { Text(deck.name) },
                                        onClick = {
                                            selectedDeckId = deck.id
                                            isDeckDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Card Creation Type Options (Section 9)
                        Text("Card Type:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = cardCreationType == "OCR",
                                onClick = { cardCreationType = "OCR" },
                                label = { Text("OCR (AI Text)") }
                            )
                            FilterChip(
                                selected = cardCreationType == "LOCAL_IMAGE",
                                onClick = { cardCreationType = "LOCAL_IMAGE" },
                                label = { Text("Local Photo Card") }
                            )
                        }

                        if (cardCreationType == "OCR") {
                            // Temporary Prompt Addendum (Section 3)
                            OutlinedTextField(
                                value = temporaryPromptAddendum,
                                onValueChange = { temporaryPromptAddendum = it },
                                label = { Text("Temporary Prompt Note (Optional)") },
                                placeholder = { Text("e.g., Focus on equations or vocabulary only") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Extraction mode
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedExplainMode = !selectedExplainMode }
                            ) {
                                Checkbox(
                                    checked = selectedExplainMode,
                                    onCheckedChange = { selectedExplainMode = it },
                                    colors = CheckboxDefaults.colors(checkedColor = NeumorphicColors.Primary)
                                )
                                Text("Mode Explication (IA Q&R Synthesis)", fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                            }
                        } else {
                            // Title override for local photo card
                            OutlinedTextField(
                                value = temporaryPromptAddendum,
                                onValueChange = { temporaryPromptAddendum = it },
                                label = { Text("Card Title (Optional)") },
                                placeholder = { Text("Leave blank for AI auto-generated title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val bitmapToProcess = pendingCroppedBitmap!!
                            val targetDeck = selectedDeckId
                            val promptAddendumVal = temporaryPromptAddendum.trim().ifBlank { null }
                            showModeDialog = false
                            pendingCroppedBitmap = null

                            if (cardCreationType == "OCR") {
                                viewModel.processCapturedImageWithCustomPrompt(
                                    croppedBitmap = bitmapToProcess,
                                    temporaryPromptAddendum = promptAddendumVal,
                                    explainMode = selectedExplainMode,
                                    targetDeckId = targetDeck
                                ) { result, err ->
                                    try {
                                        if (sourceFile.exists()) sourceFile.delete()
                                    } catch (e: Exception) {
                                        android.util.Log.e("CropEditorScreen", "Failed to delete source capture file", e)
                                    }

                                    if (result != null) {
                                        val note = result.toSingleNote(targetDeck, viewModel.uiState.value.srsSettings.startingEaseFactor)
                                        viewModel.updateNote(note)
                                        Toast.makeText(context, "Carte créée avec succès ! ✨", Toast.LENGTH_SHORT).show()
                                        onCropConfirmed()
                                    } else {
                                        errorMessageToShow = err ?: "Échec de l'extraction. Veuillez vérifier l'image ou la clé API."
                                    }
                                }
                            } else {
                                // Create local photo card
                                viewModel.createLocalImageCard(
                                    bitmap = bitmapToProcess,
                                    userTitle = promptAddendumVal,
                                    deckId = targetDeck
                                ) { success, err ->
                                    try {
                                        if (sourceFile.exists()) sourceFile.delete()
                                    } catch (e: Exception) {}

                                    if (success) {
                                        Toast.makeText(context, "Photo Card créée avec succès ! 📷", Toast.LENGTH_SHORT).show()
                                        onCropConfirmed()
                                    } else {
                                        errorMessageToShow = err ?: "Failed to save photo card."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Card")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showModeDialog = false
                            pendingCroppedBitmap = null
                        }
                    ) {
                        Text("Cancel", color = NeumorphicColors.TextSecondary)
                    }
                },
                containerColor = NeumorphicColors.SurfaceLight
            )
        }

        // Detailed OCR Error Dialog
        if (errorMessageToShow != null) {
            AlertDialog(
                onDismissRequest = { errorMessageToShow = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OCR / Scan Error",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = errorMessageToShow!!,
                            fontSize = 14.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tip: Make sure the cropped selection is clear, oriented right-side up, or enter a Gemini API Key in Settings.",
                            fontSize = 12.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { errorMessageToShow = null },
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                    ) {
                        Text("OK")
                    }
                },
                containerColor = NeumorphicColors.SurfaceLight
            )
        }

        // Loading Overlay
        if (state.isProcessingOcr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeumorphicColors.SurfaceLight),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NeumorphicColors.Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Processing OCR...",
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Extracting text and generating card...",
                            fontSize = 12.sp,
                            color = NeumorphicColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun cropBitmap(
    originalBitmap: Bitmap,
    mode: CropMode,
    rectNormLeft: Float,
    rectNormTop: Float,
    rectNormRight: Float,
    rectNormBottom: Float,
    lassoNormPoints: List<Offset>
): Bitmap {
    val w = originalBitmap.width
    val h = originalBitmap.height

    return if (mode == CropMode.RECTANGLE) {
        val x = (rectNormLeft * w).toInt().coerceIn(0, w - 1)
        val y = (rectNormTop * h).toInt().coerceIn(0, h - 1)
        val cropW = ((rectNormRight - rectNormLeft) * w).toInt().coerceIn(1, w - x)
        val cropH = ((rectNormBottom - rectNormTop) * h).toInt().coerceIn(1, h - y)

        Bitmap.createBitmap(originalBitmap, x, y, cropW, cropH)
    } else {
        if (lassoNormPoints.size < 3) {
            return originalBitmap
        }

        val resultBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
        }

        val path = android.graphics.Path()
        val p0 = lassoNormPoints.first()
        path.moveTo(p0.x * w, p0.y * h)
        for (i in 1 until lassoNormPoints.size) {
            val pt = lassoNormPoints[i]
            path.lineTo(pt.x * w, pt.y * h)
        }
        path.close()

        canvas.drawPath(path, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

        // Find bounding box of lasso path
        val bounds = android.graphics.RectF()
        path.computeBounds(bounds, true)

        val bx = bounds.left.toInt().coerceIn(0, w - 1)
        val by = bounds.top.toInt().coerceIn(0, h - 1)
        val bw = bounds.width().toInt().coerceIn(1, w - bx)
        val bh = bounds.height().toInt().coerceIn(1, h - by)

        Bitmap.createBitmap(resultBitmap, bx, by, bw, bh)
    }
}

private fun loadOrientedBitmap(filePath: String): Bitmap? {
    val file = File(filePath)
    if (!file.exists()) return null
    val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
    return try {
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees != 0f) {
            val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } else {
            bitmap
        }
    } catch (e: Exception) {
        bitmap
    }
}
