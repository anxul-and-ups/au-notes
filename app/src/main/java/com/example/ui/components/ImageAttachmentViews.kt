package com.example.ui.components

import androidx.compose.foundation.clickable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.AttachmentRenderer
import com.example.ui.util.RichTextFormatter.AttachmentInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val imageBitmapCache = androidx.collection.LruCache<String, ImageBitmap>(40)

// ---------------------------------------------------------------------------------------------
// Responsibilities of the three attachment values (shared by the inline editor and the dialog):
//   widthFraction     -> LAYOUT FOOTPRINT: width of the frame as a fraction of the note width.
//   zoom              -> zoom of the picture INSIDE its frame. Never below 1, because a picture that
//                        is smaller than its frame leaves an empty rectangle around it.
//   posX / posY       -> move of the picture INSIDE its frame, as a fraction of the frame width/height
//                        (same unit the renderer/export uses). Limited so the picture always covers
//                        the whole frame.
// ---------------------------------------------------------------------------------------------
const val MIN_IMAGE_ZOOM = 1f
const val MAX_IMAGE_ZOOM = 5f

/** Largest |posX| / |posY| that still keeps a picture zoomed by [zoom] covering its whole frame. */
fun imageMaxPan(zoom: Float): Float = (zoom.coerceIn(MIN_IMAGE_ZOOM, MAX_IMAGE_ZOOM) - 1f) / 2f

fun clampImagePan(value: Float, zoom: Float): Float {
    val m = imageMaxPan(zoom)
    return value.coerceIn(-m, m)
}

/** Sets the inner zoom (clamped) and re-clamps the pan so the picture still covers its frame. */
fun AttachmentInfo.withInnerZoom(newZoom: Float): AttachmentInfo {
    val z = newZoom.coerceIn(MIN_IMAGE_ZOOM, MAX_IMAGE_ZOOM)
    return copy(zoom = z, posX = clampImagePan(posX, z), posY = clampImagePan(posY, z))
}

/** Moves the picture inside its frame by [dx]/[dy] (fractions of frame width/height), clamped. */
fun AttachmentInfo.withInnerPan(dx: Float, dy: Float): AttachmentInfo =
    copy(posX = clampImagePan(posX + dx, zoom), posY = clampImagePan(posY + dy, zoom))

/** Rotated + cropped bitmap for [att], cached in memory so typing never causes images to blink. */
@Composable
private fun rememberBaseImage(att: AttachmentInfo, maxSide: Int, debounceMs: Long = 0L): ImageBitmap? {
    val cacheKey = "${att.uri}_${att.rotation}_${att.cropLeft}_${att.cropTop}_${att.cropRight}_${att.cropBottom}_$maxSide"
    val cached = imageBitmapCache.get(cacheKey)
    val state = produceState<ImageBitmap?>(
        initialValue = cached,
        cacheKey
    ) {
        if (cached == null) {
            if (debounceMs > 0) delay(debounceMs)
            val bmp = withContext(Dispatchers.IO) { AttachmentRenderer.baseBitmap(att, maxSide)?.asImageBitmap() }
            if (bmp != null) {
                imageBitmapCache.put(cacheKey, bmp)
            }
            value = bmp
        } else {
            value = cached
        }
    }
    return state.value ?: cached
}

/**
 * Draws an image exactly the way it is exported: rotation + crop define the frame's shape, then
 * zoom and offset move the picture inside that frame. [modifier] decides the frame WIDTH; the
 * height follows from the aspect ratio.
 */
@Composable
fun AttachmentFrame(
    att: AttachmentInfo,
    modifier: Modifier = Modifier,
    maxSide: Int = 1400,
    debounceMs: Long = 0L,
    cornerRadius: Int = 6
) {
    val base = rememberBaseImage(att, maxSide, debounceMs)
    var frameW by remember { mutableFloatStateOf(1f) }
    var frameH by remember { mutableFloatStateOf(1f) }
    // The base bitmap is already rotated + cropped, so its own proportions ARE the visible frame's
    // proportions. Using them makes it impossible for the picture to be stretched or letter-boxed;
    // frameAspect(att) is only used until the bitmap has loaded.
    val frameAspect = if (base != null && base.width > 0 && base.height > 0) {
        base.width.toFloat() / base.height.toFloat()
    } else {
        AttachmentRenderer.frameAspect(att)
    }
    // Old notes may hold zoom < 1 or a pan from the former Left/Right buttons; render them valid.
    val shownZoom = att.zoom.coerceIn(MIN_IMAGE_ZOOM, MAX_IMAGE_ZOOM)
    val shownPosX = clampImagePan(att.posX, shownZoom)
    val shownPosY = clampImagePan(att.posY, shownZoom)
    Box(
        modifier = modifier
            .aspectRatio(frameAspect)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color(0x14808080))
            .onSizeChanged { frameW = it.width.toFloat(); frameH = it.height.toFloat() }
    ) {
        if (base != null) {
            Image(
                bitmap = base,
                contentDescription = att.fileName,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = shownZoom
                        scaleY = shownZoom
                        translationX = shownPosX * frameW
                        translationY = shownPosY * frameH
                    }
            )
        }
    }
}

/**
 * Full image editor: drag to move, pinch / buttons to zoom, size slider, crop sliders,
 * rotate, lock. Nothing is applied until the user taps the check mark; the original file is
 * never modified. A locked image ignores every edit control until it is unlocked.
 */
@Composable
fun ImageEditorDialog(
    initial: AttachmentInfo,
    isDarkMode: Boolean,
    showSizeControl: Boolean = true,
    onDismiss: () -> Unit,
    onApply: (AttachmentInfo) -> Unit,
    onDelete: () -> Unit
) {
    var draft by remember { mutableStateOf(initial.withInnerZoom(initial.zoom)) }
    val locked = draft.locked
    val textColor = if (isDarkMode) Color.White else Color(0xFF111111)
    val sub = if (isDarkMode) Color.White.copy(0.6f) else Color(0xFF666666)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isDarkMode) Color(0xFF0E1015) else Color(0xFFF6F8FB)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // ---- top bar ----
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel", tint = textColor) }
                    Text("Edit image", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Row {
                        IconButton(onClick = { draft = draft.copy(locked = !draft.locked) }) {
                            Icon(
                                if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                if (locked) "Unlock image" else "Lock image",
                                tint = if (locked) CrimsonPrimary else textColor
                            )
                        }
                        IconButton(onClick = { onApply(draft) }) {
                            Icon(Icons.Default.Check, "Apply", tint = CrimsonPrimary)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // ---- preview with gestures ----
                    val aspect = AttachmentRenderer.frameAspect(draft)
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val maxPreviewH = 340.dp
                        val previewW = minOf(maxWidth, maxPreviewH * aspect)
                        var pw by remember { mutableFloatStateOf(1f) }
                        var ph by remember { mutableFloatStateOf(1f) }
                        AttachmentFrame(
                            att = draft,
                            maxSide = 1000,
                            debounceMs = 40L,
                            cornerRadius = 10,
                            modifier = Modifier
                                .width(previewW)
                                .border(2.dp, if (locked) Color.Gray else CrimsonPrimary, RoundedCornerShape(10.dp))
                                .onSizeChanged { pw = it.width.toFloat(); ph = it.height.toFloat() }
                                .pointerInput(locked) {
                                    if (!locked) {
                                        detectTransformGestures { _, pan, zoomChange, _ ->
                                            // pinch = zoom inside the frame, drag = move inside the frame
                                            // (the frame's own size is the "Size" slider, never the pinch)
                                            draft = draft
                                                .withInnerZoom(draft.zoom * zoomChange)
                                                .withInnerPan(pan.x / pw, pan.y / ph)
                                        }
                                    }
                                }
                        )
                    }
                    Text(
                        if (locked) "Locked — unlock to edit" else "Drag to move • Pinch to zoom",
                        fontSize = 11.sp, color = if (locked) CrimsonPrimary else sub,
                        modifier = Modifier.padding(top = 6.dp).align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(14.dp))

                    // ---- rotate / zoom ----
                    Row(
                        modifier = Modifier.fillMaxWidth().alpha(if (locked) 0.35f else 1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ToolButton(Icons.Default.RotateLeft, "Rotate left", !locked, textColor) {
                            draft = draft.copy(rotation = (draft.rotation + 270) % 360, posX = 0f, posY = 0f,
                                cropLeft = 0f, cropTop = 0f, cropRight = 0f, cropBottom = 0f)
                        }
                        ToolButton(Icons.Default.RotateRight, "Rotate right", !locked, textColor) {
                            draft = draft.copy(rotation = (draft.rotation + 90) % 360, posX = 0f, posY = 0f,
                                cropLeft = 0f, cropTop = 0f, cropRight = 0f, cropBottom = 0f)
                        }
                        ToolButton(Icons.Default.ZoomOut, "Zoom out", !locked, textColor) {
                            draft = draft.withInnerZoom(draft.zoom - 0.1f)
                        }
                        ToolButton(Icons.Default.ZoomIn, "Zoom in", !locked, textColor) {
                            draft = draft.withInnerZoom(draft.zoom + 0.1f)
                        }
                        ToolButton(Icons.Default.Refresh, "Reset", !locked, CrimsonPrimary) {
                            draft = draft.copy(
                                zoom = 1f, posX = 0f, posY = 0f, rotation = 0, widthFraction = 1f,
                                cropLeft = 0f, cropTop = 0f, cropRight = 0f, cropBottom = 0f
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Zoom  ${"%.0f".format(draft.zoom * 100)}%", fontSize = 12.sp, color = sub)
                    EditSlider(draft.zoom, MIN_IMAGE_ZOOM..MAX_IMAGE_ZOOM, !locked) { draft = draft.withInnerZoom(it) }

                    // Full Page: the image gets its own A4 page (fits inside, keeps its aspect ratio; also in the PDF)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (draft.fullPage) CrimsonPrimary.copy(alpha = 0.16f) else Color.Gray.copy(alpha = 0.10f))
                            .clickable(enabled = !locked) {
                                draft = if (draft.fullPage) draft.copy(fullPage = false)
                                else draft.copy(fullPage = true, widthFraction = 1f, zoom = 1f, posX = 0f, posY = 0f)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Full Page", fontWeight = FontWeight.SemiBold, color = if (draft.fullPage) CrimsonPrimary else textColor, fontSize = 14.sp)
                            Text("Image takes one whole page (fits inside, never stretched)", fontSize = 11.sp, color = sub)
                        }
                        Switch(
                            checked = draft.fullPage,
                            enabled = !locked,
                            onCheckedChange = { on ->
                                draft = if (on) draft.copy(fullPage = true, widthFraction = 1f, zoom = 1f, posX = 0f, posY = 0f) else draft.copy(fullPage = false)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                        )
                    }

                    if (showSizeControl && !draft.fullPage) {
                        Text("Size  ${"%.0f".format(draft.widthFraction * 100)}% of note width", fontSize = 12.sp, color = sub)
                        EditSlider(draft.widthFraction, 0.2f..1f, !locked) { draft = draft.copy(widthFraction = it) }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text("CROP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary, letterSpacing = 1.sp)
                    CropRow("Left", draft.cropLeft, !locked, sub) { draft = draft.copy(cropLeft = it.coerceAtMost(0.9f - draft.cropRight)) }
                    CropRow("Top", draft.cropTop, !locked, sub) { draft = draft.copy(cropTop = it.coerceAtMost(0.9f - draft.cropBottom)) }
                    CropRow("Right", draft.cropRight, !locked, sub) { draft = draft.copy(cropRight = it.coerceAtMost(0.9f - draft.cropLeft)) }
                    CropRow("Bottom", draft.cropBottom, !locked, sub) { draft = draft.copy(cropBottom = it.coerceAtMost(0.9f - draft.cropTop)) }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = draft.fileName,
                        onValueChange = { draft = draft.copy(fileName = it) },
                        label = { Text("File name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Lock image", fontWeight = FontWeight.SemiBold, color = textColor, fontSize = 14.sp)
                            Text("Prevents moving, resizing, rotating and cropping", fontSize = 11.sp, color = sub)
                        }
                        Switch(
                            checked = locked,
                            onCheckedChange = { draft = draft.copy(locked = it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CrimsonPrimary)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1FFF5252))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (!locked) onDelete() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, "Remove image", tint = if (locked) Color.Gray else Color(0xFFFF5252))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (locked) "Unlock to remove" else "Remove image from note",
                            color = if (locked) Color.Gray else Color(0xFFFF5252), fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, label, tint = tint) }
}

@Composable
private fun EditSlider(value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, onChange: (Float) -> Unit) {
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        valueRange = range,
        enabled = enabled,
        colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
    )
}

@Composable
private fun CropRow(label: String, value: Float, enabled: Boolean, sub: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = sub, modifier = Modifier.width(52.dp))
        Box(Modifier.weight(1f)) { EditSlider(value, 0f..0.45f, enabled, onChange) }
        Text("${(value * 100).toInt()}%", fontSize = 11.sp, color = sub, modifier = Modifier.width(36.dp))
    }
}
