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

    // Rectangle crop state (normalized 0..1 scale)
    var rectNormLeft by remember { mutableStateOf(0.1f) }
    var rectNormTop by remember { mutableStateOf(0.2f) }
    var rectNormRight by remember { mutableStateOf(0.9f) }
    var rectNormBottom by remember { mutableStateOf(0.8f) }

    // Lasso path points state (normalized 0..1 scale)
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

            // Draw image scaled to fit
            Image(
                bitmap = workingBitmap!!.asImageBitmap(),
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
                                    val rLeft = currentLeft * containerWidthPx
                                    val rTop = currentTop * containerHeightPx
                                    val rRight = currentRight * containerWidthPx
                                    val rBottom = currentBottom * containerHeightPx
                                    val threshold = 60f

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
                                    val dx = dragAmount.x / containerWidthPx
                                    val dy = dragAmount.y / containerHeightPx

                                    when (draggedHandle) {
                                        HandleType.TOP_LEFT -> {
                                            rectNormLeft = (currentLeft + dx).coerceIn(0f, currentRight - 0.1f)
                                            rectNormTop = (currentTop + dy).coerceIn(0f, currentBottom - 0.1f)
                                        }
                                        HandleType.TOP_RIGHT -> {
                                            rectNormRight = (currentRight + dx).coerceIn(currentLeft + 0.1f, 1f)
                                            rectNormTop = (currentTop + dy).coerceIn(0f, currentBottom - 0.1f)
                                        }
                                        HandleType.BOTTOM_LEFT -> {
                                            rectNormLeft = (currentLeft + dx).coerceIn(0f, currentRight - 0.1f)
                                            rectNormBottom = (currentBottom + dy).coerceIn(currentTop + 0.1f, 1f)
                                        }
                                        HandleType.BOTTOM_RIGHT -> {
                                            rectNormRight = (currentRight + dx).coerceIn(currentLeft + 0.1f, 1f)
                                            rectNormBottom = (currentBottom + dy).coerceIn(currentTop + 0.1f, 1f)
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
                    val rLeft = rectNormLeft * containerWidthPx
                    val rTop = rectNormTop * containerHeightPx
                    val rRight = rectNormRight * containerWidthPx
                    val rBottom = rectNormBottom * containerHeightPx

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

                    // Handles
                    listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { handle ->
                        drawCircle(color = Color.White, radius = 16f, center = handle)
                        drawCircle(color = primaryColor, radius = 12f, center = handle)
                    }
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val nx = offset.x / containerWidthPx
                                    val ny = offset.y / containerHeightPx
                                    lassoNormPoints = listOf(Offset(nx, ny))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val nx = change.position.x / containerWidthPx
                                    val ny = change.position.y / containerHeightPx
                                    lassoNormPoints = lassoNormPoints + Offset(nx, ny)
                                }
                            )
                        }
                ) {
                    if (lassoNormPoints.size > 1) {
                        val path = Path().apply {
                            val first = lassoNormPoints.first()
                            moveTo(first.x * containerWidthPx, first.y * containerHeightPx)
                            for (i in 1 until lassoNormPoints.size) {
                                val pt = lassoNormPoints[i]
                                lineTo(pt.x * containerWidthPx, pt.y * containerHeightPx)
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
            color = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (cropMode == CropMode.RECTANGLE) "Adjust crop area" else "Trace text with your finger",
                    color = Color.White,
                    fontSize = 13.sp
                )

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
                    colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm")
                }
            }
        }

        // Extraction Mode Dialog
        if (showModeDialog && pendingCroppedBitmap != null) {
            AlertDialog(
                onDismissRequest = {
                    showModeDialog = false
                    pendingCroppedBitmap = null
                },
                containerColor = NeumorphicColors.DialogBackground,
                title = { Text("Card Extraction Mode", fontWeight = FontWeight.Bold, color = NeumorphicColors.TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Choose how to extract text for this card:",
                            fontSize = 13.sp,
                            color = NeumorphicColors.TextSecondary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedExplainMode = false }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = !selectedExplainMode,
                                onClick = { selectedExplainMode = false },
                                colors = RadioButtonDefaults.colors(selectedColor = NeumorphicColors.Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Verbatim (As it is)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeumorphicColors.TextPrimary)
                                Text("Extract exact text as it appears in photo", fontSize = 12.sp, color = NeumorphicColors.TextSecondary)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedExplainMode = true }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedExplainMode,
                                onClick = { selectedExplainMode = true },
                                colors = RadioButtonDefaults.colors(selectedColor = NeumorphicColors.Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Explain Mode (AI Q&A)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeumorphicColors.TextPrimary)
                                Text("Synthesize text into a structured study Q&A", fontSize = 12.sp, color = NeumorphicColors.TextSecondary)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val bitmapToProcess = pendingCroppedBitmap!!
                            val useExplain = selectedExplainMode
                            showModeDialog = false
                            pendingCroppedBitmap = null

                            viewModel.processCapturedImage(bitmapToProcess, explainMode = useExplain) { success ->
                                try {
                                    if (sourceFile.exists()) {
                                        sourceFile.delete()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CropEditorScreen", "Failed to delete source capture file", e)
                                }

                                if (success) {
                                    Toast.makeText(context, "Card created successfully! ✨", Toast.LENGTH_SHORT).show()
                                    onCropConfirmed()
                                } else {
                                    val errorMsg = viewModel.uiState.value.ocrError.takeIf { !it.isNullOrBlank() }
                                        ?: "Failed to extract text from image. Please ensure text is legible."
                                    errorMessageToShow = errorMsg
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                    ) {
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
                }
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
                containerColor = NeumorphicColors.DialogBackground
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
