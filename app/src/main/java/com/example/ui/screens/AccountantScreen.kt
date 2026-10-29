package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MilkHisabCalculator
import com.example.data.model.MilkRecordEntity
import com.example.data.repository.MilkAccountingRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.MilkHisabPdfExporter
import com.example.ui.util.SavedExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

@Composable
fun AccountantScreen(
    repository: MilkAccountingRepository,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize with live local device calendar (Section 3: First launch behaviour)
    val liveCalendar = remember { Calendar.getInstance() }
    val currentDeviceYear = remember { liveCalendar.get(Calendar.YEAR) }
    val currentDeviceMonth = remember { liveCalendar.get(Calendar.MONTH) + 1 } // 1..12

    var selectedYear by remember { mutableIntStateOf(currentDeviceYear) }
    var selectedMonth by remember { mutableIntStateOf(currentDeviceMonth) }

    // Fetch records for current selected month reactively from Room DB
    val monthRecordsFlow = remember(selectedYear, selectedMonth) {
        repository.getRecordsForMonth(selectedYear, selectedMonth)
    }
    val recordsList by monthRecordsFlow.collectAsState(initial = emptyList())
    val recordsMap = remember(recordsList) {
        recordsList.associateBy { it.dayOfMonth }
    }

    // Deterministic totals calculation
    val summary = remember(selectedYear, selectedMonth, recordsMap) {
        MilkHisabCalculator.computeSummary(selectedYear, selectedMonth, recordsMap)
    }

    // Selected period tab (0 = All Days, 1 = Period 1, 2 = Period 2, 3 = Period 3)
    var selectedPeriodFilter by remember { mutableIntStateOf(0) }

    // Dialog state for editing a day
    var editingDay by remember { mutableStateOf<Int?>(null) }

    // Modals
    var showExportPdfModal by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<SavedExport?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    val bgGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF13151A),
                Color(0xFF181B22),
                Color(0xFF0F1115)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF7F8FA),
                Color(0xFFEFF1F5),
                Color(0xFFE5E8EE)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    isDarkMode = isDarkMode,
                    size = 40.dp,
                    iconSize = 18.dp,
                    onClick = onBack
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACCOUNTANT",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimsonPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Milk Dairy Hisab",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                    )
                }

                // PDF Export Button
                NeuIconButton(
                    icon = Icons.Default.FileDownload,
                    contentDescription = "Export PDF",
                    isDarkMode = isDarkMode,
                    size = 38.dp,
                    iconSize = 18.dp,
                    tint = CrimsonPrimary,
                    onClick = { showExportPdfModal = true }
                )
            }

            // Month Navigation Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                isDarkMode = isDarkMode,
                shape = RoundedCornerShape(16.dp),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Month Button
                    NeuIconButton(
                        icon = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        isDarkMode = isDarkMode,
                        size = 36.dp,
                        iconSize = 20.dp,
                        onClick = {
                            if (selectedMonth == 1) {
                                selectedMonth = 12
                                selectedYear -= 1
                            } else {
                                selectedMonth -= 1
                            }
                        }
                    )

                    // Month & Year Display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summary.monthName} $selectedYear",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        val isCurrentMonth = (selectedYear == currentDeviceYear && selectedMonth == currentDeviceMonth)
                        if (isCurrentMonth) {
                            Text(
                                text = "Current Month",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CrimsonPrimary
                            )
                        } else {
                            Text(
                                text = "Jump to Current",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.clickable {
                                    selectedYear = currentDeviceYear
                                    selectedMonth = currentDeviceMonth
                                }
                            )
                        }
                    }

                    // Next Month Button
                    NeuIconButton(
                        icon = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        isDarkMode = isDarkMode,
                        size = 36.dp,
                        iconSize = 20.dp,
                        onClick = {
                            if (selectedMonth == 12) {
                                selectedMonth = 1
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                        }
                    )
                }
            }

            // Monthly Grand Totals Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                isDarkMode = isDarkMode,
                shape = RoundedCornerShape(18.dp),
                strong = true,
                elevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MONTHLY TOTAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${summary.totalDays} Days",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Milk",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                            )
                            Text(
                                text = "${MilkHisabCalculator.formatQty(summary.grandTotalQty)} KG",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Earning",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                            )
                            Text(
                                text = "₹ ${MilkHisabCalculator.formatAmount(summary.grandTotalAmount)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimsonPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x1FFFFFFF) else Color(0x1F000000))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Morning vs Evening Split Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Morning",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Morning: ${MilkHisabCalculator.formatQty(summary.monthlyMorningQty)} KG (₹${MilkHisabCalculator.formatAmount(summary.monthlyMorningAmount)})",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = "Evening",
                                tint = Color(0xFF7E57C2),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Evening: ${MilkHisabCalculator.formatQty(summary.monthlyEveningQty)} KG (₹${MilkHisabCalculator.formatAmount(summary.monthlyEveningAmount)})",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black
                            )
                        }
                    }
                }
            }

            // 10-Day Period Summaries Row (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Period 1 (1–10)
                PeriodChipCard(
                    period = summary.period1,
                    isSelected = selectedPeriodFilter == 1,
                    isDarkMode = isDarkMode,
                    onClick = {
                        selectedPeriodFilter = if (selectedPeriodFilter == 1) 0 else 1
                    }
                )

                // Period 2 (11–20)
                PeriodChipCard(
                    period = summary.period2,
                    isSelected = selectedPeriodFilter == 2,
                    isDarkMode = isDarkMode,
                    onClick = {
                        selectedPeriodFilter = if (selectedPeriodFilter == 2) 0 else 2
                    }
                )

                // Period 3 (21–End)
                PeriodChipCard(
                    period = summary.period3,
                    isSelected = selectedPeriodFilter == 3,
                    isDarkMode = isDarkMode,
                    onClick = {
                        selectedPeriodFilter = if (selectedPeriodFilter == 3) 0 else 3
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Section Label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedPeriodFilter) {
                        1 -> "DAYS 1–10 (${summary.monthName})"
                        2 -> "DAYS 11–20 (${summary.monthName})"
                        3 -> "DAYS 21–${summary.totalDays} (${summary.monthName})"
                        else -> "ALL DAYS (${summary.monthName} 1–${summary.totalDays})"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonPrimary,
                    letterSpacing = 1.sp
                )

                if (selectedPeriodFilter != 0) {
                    Text(
                        text = "Show All Days",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.clickable { selectedPeriodFilter = 0 }
                    )
                }
            }

            // Daily List Table
            val filteredDays = remember(selectedPeriodFilter, summary.totalDays) {
                when (selectedPeriodFilter) {
                    1 -> (1..10.coerceAtMost(summary.totalDays)).toList()
                    2 -> (11..20.coerceAtMost(summary.totalDays)).toList()
                    3 -> (21..summary.totalDays).toList()
                    else -> (1..summary.totalDays).toList()
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDays) { day ->
                    val record = recordsMap[day]
                    DayRowCard(
                        day = day,
                        year = selectedYear,
                        month = selectedMonth,
                        monthName = summary.monthName,
                        record = record,
                        isDarkMode = isDarkMode,
                        onClick = { editingDay = day }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Modal to Edit Entry for a specific Day
    editingDay?.let { day ->
        val record = recordsMap[day]
        DayEditDialog(
            day = day,
            month = selectedMonth,
            year = selectedYear,
            monthName = summary.monthName,
            existingRecord = record,
            isDarkMode = isDarkMode,
            onDismiss = { editingDay = null },
            onSave = { mQty, mAmt, eQty, eAmt ->
                coroutineScope.launch {
                    val dateKey = MilkHisabCalculator.formatDateKey(selectedYear, selectedMonth, day)
                    val newEntity = MilkRecordEntity(
                        date = dateKey,
                        year = selectedYear,
                        month = selectedMonth,
                        dayOfMonth = day,
                        morningQty = mQty,
                        morningAmount = mAmt,
                        eveningQty = eQty,
                        eveningAmount = eAmt,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.saveRecord(newEntity)
                    editingDay = null
                    Toast.makeText(context, "Saved Day $day Hisab", Toast.LENGTH_SHORT).show()
                }
            },
            onClear = {
                coroutineScope.launch {
                    val dateKey = MilkHisabCalculator.formatDateKey(selectedYear, selectedMonth, day)
                    repository.deleteRecordByDate(dateKey)
                    editingDay = null
                    Toast.makeText(context, "Cleared Day $day Hisab", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Export PDF Modal
    if (showExportPdfModal) {
        ExportPdfDialog(
            year = selectedYear,
            month = selectedMonth,
            summary = summary,
            isDarkMode = isDarkMode,
            isExporting = isExporting,
            exportResult = exportResult,
            onDismiss = {
                showExportPdfModal = false
                exportResult = null
            },
            onExport = { periodFilter ->
                isExporting = true
                coroutineScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        MilkHisabPdfExporter.exportMonthPdf(
                            context = context,
                            year = selectedYear,
                            month = selectedMonth,
                            recordsMap = recordsMap,
                            periodFilter = periodFilter
                        )
                    }
                    isExporting = false
                    exportResult = result
                    if (result != null) {
                        Toast.makeText(context, "PDF Exported Successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to create PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onOpenPdf = { uri ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open PDF"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
                }
            },
            onSharePdf = { uri ->
                try {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Milk Hisab PDF"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Unable to share PDF", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun PeriodChipCard(
    period: com.example.data.model.PeriodSummary,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) CrimsonPrimary else Color.Transparent

    GlassCard(
        modifier = Modifier
            .width(170.dp)
            .border(if (isSelected) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        isDarkMode = isDarkMode,
        elevation = if (isSelected) 6.dp else 3.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Period ${period.periodIndex}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonPrimary
                )
                Text(
                    text = "${period.startDay}–${period.endDay}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${MilkHisabCalculator.formatQty(period.totalQty)} KG",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color.White else Color.Black
            )

            Text(
                text = "₹ ${MilkHisabCalculator.formatAmount(period.totalAmount)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CrimsonPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "S: ${MilkHisabCalculator.formatQty(period.morningQty)} | Sh: ${MilkHisabCalculator.formatQty(period.eveningQty)}",
                fontSize = 10.sp,
                color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray
            )
        }
    }
}

@Composable
private fun DayRowCard(
    day: Int,
    year: Int,
    month: Int,
    monthName: String,
    record: MilkRecordEntity?,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val calendar = remember(year, month, day) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
    }
    val dayOfWeek = remember(calendar) {
        calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH) ?: ""
    }

    val hasData = record != null && record.hasEntry

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        isDarkMode = isDarkMode,
        elevation = 3.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (hasData) {
                            Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF8E53)))
                        } else {
                            if (isDarkMode) Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF)))
                            else Brush.linearGradient(listOf(Color(0x15000000), Color(0x0A000000)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d".format(day),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (hasData) Color.White else if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.DarkGray
                    )
                    Text(
                        text = dayOfWeek,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hasData) Color.White.copy(alpha = 0.85f) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Subah & Shaam breakdown
            Column(modifier = Modifier.weight(1f)) {
                // Morning (Subah)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Morning",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Morning: ",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                        )
                        Text(
                            text = if (record?.morningQty != null) "${MilkHisabCalculator.formatQty(record.morningQty)} KG" else "0 KG",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }

                    Text(
                        text = if (record?.morningAmount != null) "₹${MilkHisabCalculator.formatAmount(record.morningAmount)}" else "₹0",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (record?.morningAmount != null) CrimsonPrimary else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Evening
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = "Evening",
                            tint = Color(0xFF9575CD),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Evening: ",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                        )
                        Text(
                            text = if (record?.eveningQty != null) "${MilkHisabCalculator.formatQty(record.eveningQty)} KG" else "0 KG",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }

                    Text(
                        text = if (record?.eveningAmount != null) "₹${MilkHisabCalculator.formatAmount(record.eveningAmount)}" else "₹0",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (record?.eveningAmount != null) CrimsonPrimary else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Daily Total
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (hasData) "${MilkHisabCalculator.formatQty(record!!.dailyTotalQty)} KG" else "0 KG",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    text = if (hasData) "₹${MilkHisabCalculator.formatAmount(record!!.dailyTotalAmount)}" else "₹0",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CrimsonPrimary
                )
            }
        }
    }
}

@Composable
private fun DayEditDialog(
    day: Int,
    month: Int,
    year: Int,
    monthName: String,
    existingRecord: MilkRecordEntity?,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (mQty: Double?, mAmt: Double?, eQty: Double?, eAmt: Double?) -> Unit,
    onClear: () -> Unit
) {
    var morningQtyText by remember {
        mutableStateOf(existingRecord?.morningQty?.let { MilkHisabCalculator.formatQty(it) } ?: "")
    }
    var morningAmountText by remember {
        mutableStateOf(existingRecord?.morningAmount?.let { MilkHisabCalculator.formatAmount(it) } ?: "")
    }
    var eveningQtyText by remember {
        mutableStateOf(existingRecord?.eveningQty?.let { MilkHisabCalculator.formatQty(it) } ?: "")
    }
    var eveningAmountText by remember {
        mutableStateOf(existingRecord?.eveningAmount?.let { MilkHisabCalculator.formatAmount(it) } ?: "")
    }

    var showClearConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            isDarkMode = isDarkMode,
            strong = true,
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Day $day $monthName $year",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary
                        )
                        Text(
                            text = "Enter Morning & Evening Hisab",
                            fontSize = 11.5.sp,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Morning
                Text(
                    text = "☀️ MORNING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB300),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = morningQtyText,
                        onValueChange = { input ->
                            // Allow valid positive decimals
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                morningQtyText = input
                            }
                        },
                        label = { Text("Quantity (KG)") },
                        placeholder = { Text("e.g. 4.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color.LightGray
                        )
                    )

                    OutlinedTextField(
                        value = morningAmountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                morningAmountText = input
                            }
                        },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("e.g. 240") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color.LightGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Evening
                Text(
                    text = "🌙 EVENING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9575CD),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = eveningQtyText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                eveningQtyText = input
                            }
                        },
                        label = { Text("Quantity (KG)") },
                        placeholder = { Text("e.g. 5.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color.LightGray
                        )
                    )

                    OutlinedTextField(
                        value = eveningAmountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                eveningAmountText = input
                            }
                        },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("e.g. 270") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color.LightGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Daily Preview
                val mQ = morningQtyText.toDoubleOrNull() ?: 0.0
                val mA = morningAmountText.toDoubleOrNull() ?: 0.0
                val eQ = eveningQtyText.toDoubleOrNull() ?: 0.0
                val eA = eveningAmountText.toDoubleOrNull() ?: 0.0
                val liveTotalQty = mQ + eQ
                val liveTotalAmt = mA + eA

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDarkMode) Color(0x15FFFFFF) else Color(0x0A000000))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Total: ${MilkHisabCalculator.formatQty(liveTotalQty)} KG",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        Text(
                            text = "₹ ${MilkHisabCalculator.formatAmount(liveTotalAmt)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existingRecord?.hasEntry == true) {
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFF5252)))
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.DarkGray)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                val parsedMQty = morningQtyText.toDoubleOrNull()
                                val parsedMAmt = morningAmountText.toDoubleOrNull()
                                val parsedEQty = eveningQtyText.toDoubleOrNull()
                                val parsedEAmt = eveningAmountText.toDoubleOrNull()
                                onSave(parsedMQty, parsedMAmt, parsedEQty, parsedEAmt)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Text("Save Entry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Day $day Hisab?") },
            text = { Text("Are you sure you want to delete entries for this day? This will update period and monthly totals.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExportPdfDialog(
    year: Int,
    month: Int,
    summary: com.example.data.model.MonthlyHisabSummary,
    isDarkMode: Boolean,
    isExporting: Boolean,
    exportResult: SavedExport?,
    onDismiss: () -> Unit,
    onExport: (periodFilter: Int) -> Unit,
    onOpenPdf: (Uri) -> Unit,
    onSharePdf: (Uri) -> Unit
) {
    var selectedPeriod by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            isDarkMode = isDarkMode,
            strong = true,
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Dairy Bill PDF",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkMode) Color.White else Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select Export Period:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                val options = listOf(
                    0 to "Full Month (Days 1–${summary.totalDays})",
                    1 to "Period 1 (Days 1–10)",
                    2 to "Period 2 (Days 11–20)",
                    3 to "Period 3 (Days 21–${summary.totalDays})"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEach { (filterVal, label) ->
                        val isSelected = selectedPeriod == filterVal
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) CrimsonPrimary.copy(alpha = 0.15f)
                                    else if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CrimsonPrimary else (if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedPeriod = filterVal }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, if (isSelected) CrimsonPrimary else Color.Gray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CrimsonPrimary)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CrimsonPrimary else (if (isDarkMode) Color.White else Color.Black)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isExporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = CrimsonPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Generating PDF document...", fontSize = 12.sp, color = Color.Gray)
                    }
                } else if (exportResult != null) {
                    // Export Success Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "✓ Export Successful!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saved to: ${exportResult.displayPath}",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        exportResult.uri?.let { uri ->
                            Button(
                                onClick = { onOpenPdf(uri) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open PDF", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { onSharePdf(uri) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { onExport(selectedPeriod) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Export PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
