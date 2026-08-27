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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeumorphicColors
import com.example.data.repository.OcrEngineChoice
import com.example.data.repository.toSingleNote
import java.io.File

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class CropMode { RECTANGLE, LASSO, QUADRILATERAL }
enum class HandleType { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

@OptIn(ExperimentalMaterial3Api::class)
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

    // Lasso path points state
    var lassoNormPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Quadrilateral 4 corners state
    var quadNormTL by remember { mutableStateOf(Offset(0.15f, 0.2f)) }
    var quadNormTR by remember { mutableStateOf(Offset(0.85f, 0.2f)) }
    var quadNormBR by remember { mutableStateOf(Offset(0.85f, 0.8f)) }
    var quadNormBL by remember { mutableStateOf(Offset(0.15f, 0.8f)) }

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

                    clipRect(rect.left, rect.top, rect.right, rect.bottom, clipOp = ClipOp.Difference) {
                        drawRect(Color.Black.copy(alpha = 0.6f))
                    }

                    drawRect(
                        color = Color.White,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = 4f)
                    )

                    listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { handle ->
                        drawCircle(color = Color.White, radius = 18f, center = handle)
                        drawCircle(color = primaryColor, radius = 14f, center = handle)
                    }
                }
            } else if (cropMode == CropMode.LASSO) {
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
                    if (lassoNormPoints.size >= 2) {
                        val path = Path().apply {
                            val p0 = lassoNormPoints.first()
                            moveTo(normToScreenX(p0.x), normToScreenY(p0.y))
                            for (i in 1 until lassoNormPoints.size) {
                                val pt = lassoNormPoints[i]
                                lineTo(normToScreenX(pt.x), normToScreenY(pt.y))
                            }
                            close()
                        }

                        clipPath(path, clipOp = ClipOp.Difference) {
                            drawRect(Color.Black.copy(alpha = 0.6f))
                        }

                        drawPath(path = path, color = Color.White, style = Stroke(width = 4f))
                    } else {
                        drawRect(Color.Black.copy(alpha = 0.4f))
                    }
                }
            } else {
                // QUADRILATERAL 4-Corner Free Drag Mode
                var activeCornerIndex by remember { mutableStateOf(-1) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val pTL = Offset(normToScreenX(quadNormTL.x), normToScreenY(quadNormTL.y))
                                    val pTR = Offset(normToScreenX(quadNormTR.x), normToScreenY(quadNormTR.y))
                                    val pBR = Offset(normToScreenX(quadNormBR.x), normToScreenY(quadNormBR.y))
                                    val pBL = Offset(normToScreenX(quadNormBL.x), normToScreenY(quadNormBL.y))

                                    val threshold = 70f
                                    activeCornerIndex = when {
                                        (offset - pTL).getDistance() < threshold -> 0
                                        (offset - pTR).getDistance() < threshold -> 1
                                        (offset - pBR).getDistance() < threshold -> 2
                                        (offset - pBL).getDistance() < threshold -> 3
                                        else -> -1
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val nx = screenToNormX(change.position.x)
                                    val ny = screenToNormY(change.position.y)
                                    val newOffset = Offset(nx, ny)

                                    when (activeCornerIndex) {
                                        0 -> quadNormTL = newOffset
                                        1 -> quadNormTR = newOffset
                                        2 -> quadNormBR = newOffset
                                        3 -> quadNormBL = newOffset
                                    }
                                },
                                onDragEnd = { activeCornerIndex = -1 },
                                onDragCancel = { activeCornerIndex = -1 }
                            )
                        }
                ) {
                    val pTL = Offset(normToScreenX(quadNormTL.x), normToScreenY(quadNormTL.y))
                    val pTR = Offset(normToScreenX(quadNormTR.x), normToScreenY(quadNormTR.y))
                    val pBR = Offset(normToScreenX(quadNormBR.x), normToScreenY(quadNormBR.y))
                    val pBL = Offset(normToScreenX(quadNormBL.x), normToScreenY(quadNormBL.y))

                    val quadPath = Path().apply {
                        moveTo(pTL.x, pTL.y)
                        lineTo(pTR.x, pTR.y)
                        lineTo(pBR.x, pBR.y)
                        lineTo(pBL.x, pBL.y)
                        close()
                    }

                    clipPath(quadPath, clipOp = ClipOp.Difference) {
                        drawRect(Color.Black.copy(alpha = 0.6f))
                    }

                    drawPath(path = quadPath, color = Color.White, style = Stroke(width = 4f))

                    listOf(pTL, pTR, pBR, pBL).forEach { handle ->
                        drawCircle(color = Color.White, radius = 20f, center = handle)
                        drawCircle(color = primaryColor, radius = 15f, center = handle)
                    }
                }
            }
        }

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Text("Crop Image", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            IconButton(
                onClick = {
                    workingBitmap?.let {
                        val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                        workingBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90°", tint = Color.White)
            }
        }

        // Bottom Control Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.85f)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                // Mode Selector Bar (3 Cropping Modes)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = cropMode == CropMode.RECTANGLE,
                        onClick = { cropMode = CropMode.RECTANGLE },
                        label = { Text("Rectangle") },
                        leadingIcon = { Icon(Icons.Default.CropLandscape, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor, selectedLabelColor = Color.White)
                    )

                    FilterChip(
                        selected = cropMode == CropMode.LASSO,
                        onClick = { cropMode = CropMode.LASSO },
                        label = { Text("Lasso") },
                        leadingIcon = { Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor, selectedLabelColor = Color.White)
                    )

                    FilterChip(
                        selected = cropMode == CropMode.QUADRILATERAL,
                        onClick = { cropMode = CropMode.QUADRILATERAL },
                        label = { Text("4-Corner Quad") },
                        leadingIcon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor, selectedLabelColor = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                lassoNormPoints = lassoNormPoints,
                                quadNormTL = quadNormTL,
                                quadNormTR = quadNormTR,
                                quadNormBR = quadNormBR,
                                quadNormBL = quadNormBL
                            )

                            pendingCroppedBitmap = croppedBitmap
                            selectedExplainMode = false
                            showModeDialog = true
                        } catch (e: Exception) {
                            Toast.makeText(context, "Crop error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm Crop & Continue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Extraction Mode & Cropped Image Preview Dialog (Card Creation & Options)
        if (showModeDialog && pendingCroppedBitmap != null) {
            var selectedDeckId by remember { mutableStateOf(state.selectedDeckId) }
            var temporaryPromptAddendum by remember { mutableStateOf("") }
            var cardCreationType by remember { mutableStateOf(if (state.hasApiKey) "ML_KIT" else "ML_KIT") }
            var isDeckDropdownExpanded by remember { mutableStateOf(false) }

            var showCreateDeckInlineDialog by remember { mutableStateOf(false) }
            var inlineDeckName by remember { mutableStateOf("") }

            Dialog(
                onDismissRequest = {
                    showModeDialog = false
                    pendingCroppedBitmap = null
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = NeumorphicColors.Background,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 660.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header Title Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
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
                                IconButton(onClick = {
                                    showModeDialog = false
                                    pendingCroppedBitmap = null
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NeumorphicColors.TextPrimary)
                                }
                            }

                            // Inner Scrollable Form
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Black.copy(alpha = 0.05f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeumorphicColors.Primary.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 130.dp, max = 200.dp)
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

                                // Target Deck Selector with Inline Deck Creation
                                Text("Target Deck (Mandatory):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                                
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val activeDeckName = state.decks.find { it.id == selectedDeckId }?.name ?: "Select Deck (Required)"
                                    
                                    OutlinedButton(
                                        onClick = { isDeckDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (selectedDeckId.isBlank()) MaterialTheme.colorScheme.error else NeumorphicColors.TextPrimary
                                        )
                                    ) {
                                        Text(activeDeckName, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NeumorphicColors.Primary)
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

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Inline "+ Add New Deck" Button
                                    TextButton(
                                        onClick = { showCreateDeckInlineDialog = true },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add New Deck", fontSize = 12.sp, color = NeumorphicColors.Primary)
                                    }
                                }

                                // Card Creation Type Options
                                Text("Extraction & Card Type:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeumorphicColors.TextPrimary)
                                
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        FilterChip(
                                            selected = cardCreationType == "ML_KIT",
                                            onClick = { cardCreationType = "ML_KIT" },
                                            label = { Text("OCR (ML Kit On-Device)") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.DocumentScanner,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeumorphicColors.Primary.copy(alpha = 0.15f),
                                                selectedLabelColor = NeumorphicColors.Primary
                                            )
                                        )

                                        FilterChip(
                                            selected = cardCreationType == "GEMINI",
                                            onClick = {
                                                if (state.hasApiKey) {
                                                    cardCreationType = "GEMINI"
                                                } else {
                                                    Toast.makeText(context, "Clé Gemini API requise pour le Cloud IA. Utilisez ML Kit (gratuit & hors-ligne) !", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            label = { Text(if (state.hasApiKey) "OCR (Gemini IA)" else "OCR Gemini (Clé requise)") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        FilterChip(
                                            selected = cardCreationType == "LOCAL_IMAGE",
                                            onClick = { cardCreationType = "LOCAL_IMAGE" },
                                            label = { Text("Photo Card (Image pure)") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                if (cardCreationType == "ML_KIT") {
                                    Surface(
                                        color = NeumorphicColors.Primary.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = NeumorphicColors.Primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Google ML Kit (Modèle sur appareil) : Numérisation instantanée hors-ligne, optimisée pour les cartes en français (accents, listes, définitions).",
                                                fontSize = 12.sp,
                                                color = NeumorphicColors.TextPrimary,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = temporaryPromptAddendum,
                                        onValueChange = { temporaryPromptAddendum = it },
                                        label = { Text("Titre de la carte (Optionnel)") },
                                        placeholder = { Text("Laisser vide pour détecter le titre sur l'image") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (cardCreationType == "GEMINI") {
                                    OutlinedTextField(
                                        value = temporaryPromptAddendum,
                                        onValueChange = { temporaryPromptAddendum = it },
                                        label = { Text("Temporary Prompt Note (Optional)") },
                                        placeholder = { Text("e.g., Focus on equations or vocabulary only") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

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
                                    OutlinedTextField(
                                        value = temporaryPromptAddendum,
                                        onValueChange = { temporaryPromptAddendum = it },
                                        label = { Text("Card Title (Optional)") },
                                        placeholder = { Text("Leave blank for auto-generated title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Card Action Buttons attached to form
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showModeDialog = false
                                        pendingCroppedBitmap = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        if (selectedDeckId.isBlank()) {
                                            Toast.makeText(context, "Please select a Target Deck first!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val bitmapToProcess = pendingCroppedBitmap!!
                                        val targetDeck = selectedDeckId
                                        val promptAddendumVal = temporaryPromptAddendum.trim().ifBlank { null }
                                        showModeDialog = false
                                        pendingCroppedBitmap = null

                                        when (cardCreationType) {
                                            "ML_KIT" -> {
                                                viewModel.launchBackgroundOcrCardCreation(
                                                    croppedBitmap = bitmapToProcess,
                                                    temporaryPromptAddendum = promptAddendumVal,
                                                    explainMode = false,
                                                    targetDeckId = targetDeck,
                                                    engineChoice = OcrEngineChoice.ML_KIT
                                                )
                                                try {
                                                    if (sourceFile.exists()) sourceFile.delete()
                                                } catch (e: Exception) {}
                                                Toast.makeText(context, "Numérisation OCR lancée en arrière-plan ✨", Toast.LENGTH_SHORT).show()
                                                onCropConfirmed()
                                            }
                                            "GEMINI" -> {
                                                viewModel.launchBackgroundOcrCardCreation(
                                                    croppedBitmap = bitmapToProcess,
                                                    temporaryPromptAddendum = promptAddendumVal,
                                                    explainMode = selectedExplainMode,
                                                    targetDeckId = targetDeck,
                                                    engineChoice = OcrEngineChoice.GEMINI
                                                )
                                                try {
                                                    if (sourceFile.exists()) sourceFile.delete()
                                                } catch (e: Exception) {}
                                                Toast.makeText(context, "Analyse Gemini lancée en arrière-plan ✨", Toast.LENGTH_SHORT).show()
                                                onCropConfirmed()
                                            }
                                            else -> {
                                                viewModel.launchBackgroundLocalImageCardCreation(
                                                    bitmap = bitmapToProcess,
                                                    userTitle = promptAddendumVal,
                                                    deckId = targetDeck
                                                )
                                                try {
                                                    if (sourceFile.exists()) sourceFile.delete()
                                                } catch (e: Exception) {}
                                                Toast.makeText(context, "Enregistrement de la photo en arrière-plan 📷", Toast.LENGTH_SHORT).show()
                                                onCropConfirmed()
                                            }
                                        }
                                    },
                                    enabled = selectedDeckId.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Confirm & Create")
                                }
                            }
                        }
                    }
                }
            }

            // Inline Deck Creation Dialog
            if (showCreateDeckInlineDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDeckInlineDialog = false },
                    containerColor = NeumorphicColors.DialogBackground,
                    titleContentColor = NeumorphicColors.TextPrimary,
                    textContentColor = NeumorphicColors.TextSecondary,
                    title = { Text("Add New Deck", color = NeumorphicColors.TextPrimary) },
                    text = {
                        OutlinedTextField(
                            value = inlineDeckName,
                            onValueChange = { inlineDeckName = it },
                            label = { Text("Deck Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inlineDeckName.isNotBlank()) {
                                    viewModel.addDeckAndSelect(inlineDeckName.trim()) { newId ->
                                        selectedDeckId = newId
                                        inlineDeckName = ""
                                        showCreateDeckInlineDialog = false
                                        Toast.makeText(context, "Deck created & selected!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                        ) { Text("Create", color = Color.White) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDeckInlineDialog = false }) { Text("Cancel", color = NeumorphicColors.TextSecondary) }
                    }
                )
            }
        }

        // Detailed OCR Error Dialog
        if (errorMessageToShow != null) {
            AlertDialog(
                onDismissRequest = { errorMessageToShow = null },
                containerColor = NeumorphicColors.DialogBackground,
                titleContentColor = NeumorphicColors.TextPrimary,
                textContentColor = NeumorphicColors.TextSecondary,
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
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { errorMessageToShow = null },
                        colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                    ) {
                        Text("OK", color = Color.White)
                    }
                }
            )
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
    lassoNormPoints: List<Offset>,
    quadNormTL: Offset = Offset(0.15f, 0.2f),
    quadNormTR: Offset = Offset(0.85f, 0.2f),
    quadNormBR: Offset = Offset(0.85f, 0.8f),
    quadNormBL: Offset = Offset(0.15f, 0.8f)
): Bitmap {
    val w = originalBitmap.width
    val h = originalBitmap.height

    return when (mode) {
        CropMode.RECTANGLE -> {
            val x = (rectNormLeft * w).toInt().coerceIn(0, w - 1)
            val y = (rectNormTop * h).toInt().coerceIn(0, h - 1)
            val cropW = ((rectNormRight - rectNormLeft) * w).toInt().coerceIn(1, w - x)
            val cropH = ((rectNormBottom - rectNormTop) * h).toInt().coerceIn(1, h - y)

            Bitmap.createBitmap(originalBitmap, x, y, cropW, cropH)
        }
        CropMode.LASSO -> {
            if (lassoNormPoints.size < 3) return originalBitmap

            val resultBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
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

            val bounds = android.graphics.RectF()
            path.computeBounds(bounds, true)

            val bx = bounds.left.toInt().coerceIn(0, w - 1)
            val by = bounds.top.toInt().coerceIn(0, h - 1)
            val bw = bounds.width().toInt().coerceIn(1, w - bx)
            val bh = bounds.height().toInt().coerceIn(1, h - by)

            Bitmap.createBitmap(resultBitmap, bx, by, bw, bh)
        }
        CropMode.QUADRILATERAL -> {
            val resultBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
            val path = android.graphics.Path()

            path.moveTo(quadNormTL.x * w, quadNormTL.y * h)
            path.lineTo(quadNormTR.x * w, quadNormTR.y * h)
            path.lineTo(quadNormBR.x * w, quadNormBR.y * h)
            path.lineTo(quadNormBL.x * w, quadNormBL.y * h)
            path.close()

            canvas.drawPath(path, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

            val bounds = android.graphics.RectF()
            path.computeBounds(bounds, true)

            val bx = bounds.left.toInt().coerceIn(0, w - 1)
            val by = bounds.top.toInt().coerceIn(0, h - 1)
            val bw = bounds.width().toInt().coerceIn(1, w - bx)
            val bh = bounds.height().toInt().coerceIn(1, h - by)

            Bitmap.createBitmap(resultBitmap, bx, by, bw, bh)
        }
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
