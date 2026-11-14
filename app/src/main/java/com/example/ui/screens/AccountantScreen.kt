package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MilkAccountingRepository
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.theme.CrimsonPrimary
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Transaction type in the Hisab Kitab / Udhar Khata Ledger:
 * - DIYE: Money YOU gave/lent to someone (उधार दिए - You will get back)
 * - LIYE: Money YOU took/borrowed from someone (उधार लिए - You need to return)
 */
enum class HisabType { DIYE, LIYE }

data class HisabEntry(
    val id: Long,
    val personName: String,
    val phoneNumber: String,
    val amount: Double,
    val type: HisabType,
    val reason: String,
    val createdAt: Long,
    val isSettled: Boolean = false,
    val settledAt: Long? = null
)

object HisabKitabStore {
    private const val PREFS_NAME = "au_hisab_kitab_prefs"
    private const val KEY_ENTRIES = "hisab_entries_json"

    fun load(context: Context): List<HisabEntry> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<HisabEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    HisabEntry(
                        id = o.optLong("id", System.currentTimeMillis() + i),
                        personName = o.optString("personName", "").trim(),
                        phoneNumber = o.optString("phoneNumber", "").trim(),
                        amount = o.optDouble("amount", 0.0),
                        type = if (o.optString("type", "DIYE") == "LIYE") HisabType.LIYE else HisabType.DIYE,
                        reason = o.optString("reason", "").trim(),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        isSettled = o.optBoolean("isSettled", false),
                        settledAt = if (o.has("settledAt") && !o.isNull("settledAt")) o.optLong("settledAt") else null
                    )
                )
            }
            list.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, entries: List<HisabEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("personName", e.personName)
                    put("phoneNumber", e.phoneNumber)
                    put("amount", e.amount)
                    put("type", e.type.name)
                    put("reason", e.reason)
                    put("createdAt", e.createdAt)
                    put("isSettled", e.isSettled)
                    if (e.settledAt != null) put("settledAt", e.settledAt)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENTRIES, arr.toString())
            .apply()
    }
}

private enum class HisabTab(val label: String, val subtitle: String) {
    DASHBOARD("Dashboard", "Full Hisab Summary"),
    DIYE("उधार दिए (Lent)", "कब किसको कितने दिए"),
    LIYE("उधार लिए (Borrowed)", "कब किससे कितने लिए"),
    PEOPLE("People Khata", "Person-wise Ledger")
}

@Composable
fun AccountantScreen(
    repository: MilkAccountingRepository,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(HisabKitabStore.load(context)) }
    var activeTab by remember { mutableStateOf(HisabTab.DASHBOARD) }
    var searchQuery by remember { mutableStateOf("") }
    var showPendingOnly by remember { mutableStateOf(false) }

    // Add / Edit modal state
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<HisabEntry?>(null) }
    var defaultModalType by remember { mutableStateOf(HisabType.DIYE) }
    var prefillPersonName by remember { mutableStateOf("") }
    var prefillPhone by remember { mutableStateOf("") }

    fun persist(updated: List<HisabEntry>) {
        val sorted = updated.sortedByDescending { it.createdAt }
        entries = sorted
        HisabKitabStore.save(context, sorted)
    }

    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val sub = if (isDarkMode) Color.White.copy(alpha = 0.62f) else Color(0xFF64748B)
    val greenColor = Color(0xFF10B981) // Udhar Diye (You will receive)
    val orangeRedColor = Color(0xFFEF4444) // Udhar Liye (You have to pay)

    // Summary calculations (only pending / unsettled amounts count toward active udhar)
    val pendingEntries = remember(entries) { entries.filter { !it.isSettled } }
    val totalPendingDiye = remember(pendingEntries) {
        pendingEntries.filter { it.type == HisabType.DIYE }.sumOf { it.amount }
    }
    val totalPendingLiye = remember(pendingEntries) {
        pendingEntries.filter { it.type == HisabType.LIYE }.sumOf { it.amount }
    }
    val totalSettledDiye = remember(entries) {
        entries.filter { it.isSettled && it.type == HisabType.DIYE }.sumOf { it.amount }
    }
    val totalSettledLiye = remember(entries) {
        entries.filter { it.isSettled && it.type == HisabType.LIYE }.sumOf { it.amount }
    }
    val netBalance = totalPendingDiye - totalPendingLiye

    // Highest Udhar Pending per person ("सबसे ज्यादा किस पर उधार है")
    val personPendingDiyeMap = remember(pendingEntries) {
        pendingEntries
            .filter { it.type == HisabType.DIYE }
            .groupBy { it.personName.trim().lowercase() }
            .mapValues { (_, list) ->
                val displayName = list.first().personName
                val phone = list.firstOrNull { it.phoneNumber.isNotBlank() }?.phoneNumber.orEmpty()
                val sum = list.sumOf { it.amount }
                Triple(displayName, phone, sum)
            }
            .values
            .sortedByDescending { it.third }
    }
    val highestUdharPerson = personPendingDiyeMap.firstOrNull()

    val filteredEntries = remember(entries, activeTab, searchQuery, showPendingOnly) {
        entries.filter { e ->
            val matchesTab = when (activeTab) {
                HisabTab.DIYE -> e.type == HisabType.DIYE
                HisabTab.LIYE -> e.type == HisabType.LIYE
                else -> true
            }
            val matchesPending = !showPendingOnly || !e.isSettled
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                e.personName.lowercase().contains(q) ||
                e.phoneNumber.lowercase().contains(q) ||
                e.reason.lowercase().contains(q) ||
                formatRupees(e.amount).lowercase().contains(q)
            matchesTab && matchesPending && matchesSearch
        }
    }

    GlassBackground(isDarkMode = isDarkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeuIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            isDarkMode = isDarkMode,
                            size = 38.dp,
                            iconSize = 18.dp,
                            tint = fg,
                            onClick = onBack
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Hisab Kitab • उधार खाता",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = fg
                            )
                            Text(
                                text = "कब, किसको, कितने दिए और कब, किससे लिए • पूरा हिसाब",
                                fontSize = 11.sp,
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Quick Share Full Summary Button
                    NeuIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share Hisab Summary",
                        isDarkMode = isDarkMode,
                        size = 36.dp,
                        iconSize = 17.dp,
                        tint = CrimsonPrimary,
                        onClick = {
                            val report = buildFullHisabReport(entries, totalPendingDiye, totalPendingLiye, highestUdharPerson)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(send, "Share Hisab Kitab"))
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 4 Navigation Tabs (Dashboard, उधार दिए, उधार लिए, People Khata)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HisabTab.entries.forEach { tab ->
                        val selected = activeTab == tab
                        val tabColor = when (tab) {
                            HisabTab.DIYE -> greenColor
                            HisabTab.LIYE -> orangeRedColor
                            else -> CrimsonPrimary
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) tabColor
                                    else (if (isDarkMode) Color(0x1EFFFFFF) else Color(0x12000000))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) tabColor else (if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000)),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { activeTab = tab }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tab.label,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else fg
                                )
                                Text(
                                    text = tab.subtitle,
                                    fontSize = 9.5.sp,
                                    color = if (selected) Color.White.copy(alpha = 0.85f) else sub
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search & Pending Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name, phone, reason, amount...", fontSize = 12.sp, color = sub) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, "Clear", tint = sub, modifier = Modifier.size(15.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = if (isDarkMode) Color(0x2AFFFFFF) else Color(0x22000000),
                            focusedContainerColor = if (isDarkMode) Color(0xFF141821) else Color.White,
                            unfocusedContainerColor = if (isDarkMode) Color(0xFF141821) else Color.White,
                            focusedTextColor = fg,
                            unfocusedTextColor = fg
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (showPendingOnly) CrimsonPrimary else (if (isDarkMode) Color(0x1EFFFFFF) else Color(0x12000000)))
                            .border(
                                1.dp,
                                if (showPendingOnly) CrimsonPrimary else Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showPendingOnly = !showPendingOnly }
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (showPendingOnly) "Pending Only" else "All Entries",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showPendingOnly) Color.White else fg
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Main Content per Tab
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    if (activeTab == HisabTab.DASHBOARD) {
                        item {
                            DashboardOverviewCard(
                                isDarkMode = isDarkMode,
                                totalPendingDiye = totalPendingDiye,
                                totalPendingLiye = totalPendingLiye,
                                totalSettledDiye = totalSettledDiye,
                                totalSettledLiye = totalSettledLiye,
                                netBalance = netBalance,
                                highestUdharPerson = highestUdharPerson,
                                onQuickAddDiye = {
                                    editingEntry = null
                                    defaultModalType = HisabType.DIYE
                                    prefillPersonName = ""
                                    prefillPhone = ""
                                    showEditorDialog = true
                                },
                                onQuickAddLiye = {
                                    editingEntry = null
                                    defaultModalType = HisabType.LIYE
                                    prefillPersonName = ""
                                    prefillPhone = ""
                                    showEditorDialog = true
                                }
                            )
                        }
                    }

                    if (activeTab == HisabTab.DIYE) {
                        item {
                            SectionBannerCard(
                                title = "उधार दिए (Money You Lent)",
                                subtitle = "कब, किसको, कितने रुपये दिए और किसने वापस लौटा दिए",
                                totalLabel = "Pending to Receive: ₹${formatRupees(totalPendingDiye)}",
                                returnedLabel = "Returned so far: ₹${formatRupees(totalSettledDiye)}",
                                accentColor = greenColor,
                                isDarkMode = isDarkMode,
                                buttonText = "+ पैसे दिए (Add Lent)",
                                onAddClick = {
                                    editingEntry = null
                                    defaultModalType = HisabType.DIYE
                                    prefillPersonName = ""
                                    prefillPhone = ""
                                    showEditorDialog = true
                                }
                            )
                        }
                    }

                    if (activeTab == HisabTab.LIYE) {
                        item {
                            SectionBannerCard(
                                title = "उधार लिए (Money You Borrowed)",
                                subtitle = "कब, किससे, कितने रुपये लिए और कब वापस लौटाए",
                                totalLabel = "Pending to Pay Back: ₹${formatRupees(totalPendingLiye)}",
                                returnedLabel = "Repaid so far: ₹${formatRupees(totalSettledLiye)}",
                                accentColor = orangeRedColor,
                                isDarkMode = isDarkMode,
                                buttonText = "+ पैसे लिए (Add Borrowed)",
                                onAddClick = {
                                    editingEntry = null
                                    defaultModalType = HisabType.LIYE
                                    prefillPersonName = ""
                                    prefillPhone = ""
                                    showEditorDialog = true
                                }
                            )
                        }
                    }

                    if (activeTab == HisabTab.PEOPLE) {
                        val groupedByPerson = entries.groupBy { it.personName.trim().lowercase() }
                        if (groupedByPerson.isEmpty()) {
                            item {
                                EmptyHisabPlaceholder(
                                    isDarkMode = isDarkMode,
                                    message = "No people in your Khata yet.\nTap + below to add your first Len-Den entry."
                                )
                            }
                        } else {
                            val personCards = groupedByPerson.values.sortedByDescending { list ->
                                list.filter { !it.isSettled && it.type == HisabType.DIYE }.sumOf { it.amount }
                            }
                            items(personCards, key = { it.first().personName.lowercase() }) { personEntries ->
                                val name = personEntries.first().personName
                                val phone = personEntries.firstOrNull { it.phoneNumber.isNotBlank() }?.phoneNumber.orEmpty()
                                val pendingDiye = personEntries.filter { !it.isSettled && it.type == HisabType.DIYE }.sumOf { it.amount }
                                val pendingLiye = personEntries.filter { !it.isSettled && it.type == HisabType.LIYE }.sumOf { it.amount }
                                PersonKhataCard(
                                    name = name,
                                    phone = phone,
                                    pendingDiye = pendingDiye,
                                    pendingLiye = pendingLiye,
                                    personEntries = personEntries,
                                    isDarkMode = isDarkMode,
                                    onAddForPerson = { type ->
                                        editingEntry = null
                                        defaultModalType = type
                                        prefillPersonName = name
                                        prefillPhone = phone
                                        showEditorDialog = true
                                    },
                                    onToggleSettled = { entry ->
                                        val now = System.currentTimeMillis()
                                        persist(
                                            entries.map {
                                                if (it.id == entry.id) {
                                                    it.copy(
                                                        isSettled = !it.isSettled,
                                                        settledAt = if (!it.isSettled) now else null
                                                    )
                                                } else it
                                            }
                                        )
                                    },
                                    onCallPhone = { ph ->
                                        if (ph.isNotBlank()) {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ph")))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        if (filteredEntries.isEmpty()) {
                            item {
                                EmptyHisabPlaceholder(
                                    isDarkMode = isDarkMode,
                                    message = when (activeTab) {
                                        HisabTab.DIYE -> "कोई उधार दिए की एंट्री नहीं मिली।\nTap '+ पैसे दिए' to record money given to someone."
                                        HisabTab.LIYE -> "कोई उधार लिए की एंट्री नहीं मिली।\nTap '+ पैसे लिए' to record money borrowed from someone."
                                        else -> "अभी कोई हिसाब-किताब दर्ज नहीं है।\nTap '+ उधार दिए' or '+ उधार लिए' to start your ledger."
                                    }
                                )
                            }
                        } else {
                            items(filteredEntries, key = { it.id }) { entry ->
                                HisabTransactionCard(
                                    entry = entry,
                                    isDarkMode = isDarkMode,
                                    onMarkReturnedToggle = {
                                        val now = System.currentTimeMillis()
                                        val markSettled = !entry.isSettled
                                        persist(
                                            entries.map {
                                                if (it.id == entry.id) {
                                                    it.copy(
                                                        isSettled = markSettled,
                                                        settledAt = if (markSettled) now else null
                                                    )
                                                } else it
                                            }
                                        )
                                        Toast.makeText(
                                            context,
                                            if (markSettled) {
                                                if (entry.type == HisabType.DIYE) "लौटा दिया! Udhar marked returned with timestamp."
                                                else "वापस दे दिए! Borrowed amount marked repaid."
                                            } else "Marked as Pending again.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onEdit = {
                                        editingEntry = entry
                                        defaultModalType = entry.type
                                        showEditorDialog = true
                                    },
                                    onDelete = {
                                        persist(entries.filterNot { it.id == entry.id })
                                        Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                                    },
                                    onCall = {
                                        if (entry.phoneNumber.isNotBlank()) {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entry.phoneNumber}")))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Action Button to add a new transaction
            FloatingActionButton(
                onClick = {
                    editingEntry = null
                    defaultModalType = if (activeTab == HisabTab.LIYE) HisabType.LIYE else HisabType.DIYE
                    prefillPersonName = ""
                    prefillPhone = ""
                    showEditorDialog = true
                },
                containerColor = CrimsonPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(58.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Hisab Entry", modifier = Modifier.size(28.dp))
            }
        }
    }

    if (showEditorDialog) {
        HisabEntryDialog(
            initialEntry = editingEntry,
            defaultType = defaultModalType,
            prefillName = prefillPersonName,
            prefillPhone = prefillPhone,
            isDarkMode = isDarkMode,
            onDismiss = {
                showEditorDialog = false
                editingEntry = null
            },
            onSave = { savedEntry ->
                val updated = if (editingEntry != null) {
                    entries.map { if (it.id == savedEntry.id) savedEntry else it }
                } else {
                    listOf(savedEntry) + entries
                }
                persist(updated)
                showEditorDialog = false
                editingEntry = null
                Toast.makeText(context, "Hisab saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun DashboardOverviewCard(
    isDarkMode: Boolean,
    totalPendingDiye: Double,
    totalPendingLiye: Double,
    totalSettledDiye: Double,
    totalSettledLiye: Double,
    netBalance: Double,
    highestUdharPerson: Triple<String, String, Double>?,
    onQuickAddDiye: () -> Unit,
    onQuickAddLiye: () -> Unit
) {
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val sub = if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF64748B)
    val greenColor = Color(0xFF10B981)
    val redColor = Color(0xFFEF4444)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        isDarkMode = isDarkMode,
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "कुल लेन-देन सारांश (NET BALANCE)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = (if (netBalance >= 0) "+₹" else "-₹") + formatRupees(kotlin.math.abs(netBalance)),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (netBalance >= 0) greenColor else redColor
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background((if (netBalance >= 0) greenColor else redColor).copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (netBalance >= 0) "आपको लेने हैं (To Receive)" else "आपको देने हैं (To Pay)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netBalance >= 0) greenColor else redColor
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Two Main Columns: Udhar Diye vs Udhar Liye
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Udhar Diye Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(greenColor.copy(alpha = 0.12f))
                        .border(1.dp, greenColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, null, tint = greenColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("उधार दिए (Lent)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = greenColor)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "₹${formatRupees(totalPendingDiye)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = fg
                    )
                    Text(
                        text = "लौटाए गए: ₹${formatRupees(totalSettledDiye)}",
                        fontSize = 10.sp,
                        color = sub
                    )
                }

                // Udhar Liye Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(redColor.copy(alpha = 0.12f))
                        .border(1.dp, redColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowDownward, null, tint = redColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("उधार लिए (Borrowed)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = redColor)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "₹${formatRupees(totalPendingLiye)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = fg
                    )
                    Text(
                        text = "वापस दिए: ₹${formatRupees(totalSettledLiye)}",
                        fontSize = 10.sp,
                        color = sub
                    )
                }
            }

            // Highest Pending Udhar Card ("सबसे ज्यादा किस पर उधार है")
            if (highestUdharPerson != null && highestUdharPerson.third > 0.0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF59E0B).copy(alpha = 0.18f), CrimsonPrimary.copy(alpha = 0.14f))
                            )
                        )
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "सबसे ज्यादा उधार (Highest Pending Udhar)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = highestUdharPerson.first + (if (highestUdharPerson.second.isNotBlank()) " • ${highestUdharPerson.second}" else ""),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = fg
                            )
                        }
                    }
                    Text(
                        text = "₹${formatRupees(highestUdharPerson.third)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Quick Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onQuickAddDiye,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = greenColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("+ उधार दिए (Lent)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onQuickAddLiye,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = redColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("+ उधार लिए (Took)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionBannerCard(
    title: String,
    subtitle: String,
    totalLabel: String,
    returnedLabel: String,
    accentColor: Color,
    isDarkMode: Boolean,
    buttonText: String,
    onAddClick: () -> Unit
) {
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val sub = if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF64748B)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        isDarkMode = isDarkMode
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                    Text(subtitle, fontSize = 11.sp, color = sub)
                }
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(buttonText, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(totalLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
                Text(returnedLabel, fontSize = 11.5.sp, color = sub)
            }
        }
    }
}

@Composable
private fun HisabTransactionCard(
    entry: HisabEntry,
    isDarkMode: Boolean,
    onMarkReturnedToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit
) {
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val sub = if (isDarkMode) Color.White.copy(alpha = 0.62f) else Color(0xFF64748B)
    val isDiye = entry.type == HisabType.DIYE
    val accent = if (isDiye) Color(0xFF10B981) else Color(0xFFEF4444)

    val givenDateStr = remember(entry.createdAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.createdAt))
    }
    val settledDateStr = remember(entry.settledAt) {
        entry.settledAt?.let {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it))
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        isDarkMode = isDarkMode,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (entry.isSettled) Color.Gray.copy(alpha = 0.2f) else accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.personName.take(1).uppercase().ifBlank { "?" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (entry.isSettled) Color.Gray else accent
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = entry.personName,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.isSettled) sub else fg,
                                textDecoration = if (entry.isSettled) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (entry.isSettled) Color(0xFF10B981).copy(alpha = 0.18f) else accent.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when {
                                        entry.isSettled && isDiye -> "लौटा दिया (SETTLED)"
                                        entry.isSettled && !isDiye -> "वापस दे दिए (SETTLED)"
                                        isDiye -> "उधार दिए (LENT)"
                                        else -> "उधार लिए (TOOK)"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.isSettled) Color(0xFF10B981) else accent
                                )
                            }
                        }

                        if (entry.phoneNumber.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { onCall() }
                            ) {
                                Icon(Icons.Default.Call, null, tint = CrimsonPrimary, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = entry.phoneNumber,
                                    fontSize = 11.5.sp,
                                    color = CrimsonPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${formatRupees(entry.amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (entry.isSettled) Color.Gray else accent,
                        textDecoration = if (entry.isSettled) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = if (isDiye) "दिए: $givenDateStr" else "लिए: $givenDateStr",
                        fontSize = 10.sp,
                        color = sub
                    )
                }
            }

            if (entry.reason.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDarkMode) Color(0x16FFFFFF) else Color(0x0A000000))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "कारण (Reason): ${entry.reason}",
                        fontSize = 12.sp,
                        color = fg.copy(alpha = 0.88f)
                    )
                }
            }

            if (entry.isSettled && settledDateStr != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isDiye) "वापस मिले (Returned on): $settledDateStr" else "वापस चुकाए (Repaid on): $settledDateStr",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = if (isDarkMode) Color(0x18FFFFFF) else Color(0x14000000))
            Spacer(Modifier.height(8.dp))

            // Action Buttons: One-Tap Mark Returned / Cancel Udhar + Edit + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (entry.isSettled) Color.Gray.copy(alpha = 0.2f)
                            else Color(0xFF10B981).copy(alpha = 0.16f)
                        )
                        .border(
                            1.dp,
                            if (entry.isSettled) Color.Gray.copy(alpha = 0.4f) else Color(0xFF10B981),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onMarkReturnedToggle() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (entry.isSettled) Icons.Default.History else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (entry.isSettled) fg else Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when {
                                entry.isSettled -> "Mark Pending Again"
                                isDiye -> "लौटा दिया • Cancel Udhar"
                                else -> "वापस दे दिए • Mark Repaid"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.isSettled) fg else Color(0xFF10B981)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CrimsonPrimary, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonKhataCard(
    name: String,
    phone: String,
    pendingDiye: Double,
    pendingLiye: Double,
    personEntries: List<HisabEntry>,
    isDarkMode: Boolean,
    onAddForPerson: (HisabType) -> Unit,
    onToggleSettled: (HisabEntry) -> Unit,
    onCallPhone: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val sub = if (isDarkMode) Color.White.copy(alpha = 0.62f) else Color(0xFF64748B)
    val net = pendingDiye - pendingLiye

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        isDarkMode = isDarkMode,
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CrimsonPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(name, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = fg)
                        Text(
                            text = (if (phone.isNotBlank()) "$phone • " else "") + "${personEntries.size} entries (Tap to ${if (expanded) "hide" else "view"})",
                            fontSize = 11.sp,
                            color = sub
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = when {
                            net > 0 -> "लेने हैं: ₹${formatRupees(net)}"
                            net < 0 -> "देने हैं: ₹${formatRupees(-net)}"
                            else -> "बराबर (Settled)"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            net > 0 -> Color(0xFF10B981)
                            net < 0 -> Color(0xFFEF4444)
                            else -> Color.Gray
                        }
                    )
                    Text(
                        text = "दिए: ₹${formatRupees(pendingDiye)} | लिए: ₹${formatRupees(pendingLiye)}",
                        fontSize = 10.sp,
                        color = sub
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = if (isDarkMode) Color(0x1FFFFFFF) else Color(0x14000000))
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onAddForPerson(HisabType.DIYE) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text("+ पैसे दिए", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onAddForPerson(HisabType.LIYE) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text("+ पैसे लिए", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    if (phone.isNotBlank()) {
                        Button(
                            onClick = { onCallPhone(phone) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    personEntries.forEach { e ->
                        val dt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(e.createdAt))
                        val retDt = e.settledAt?.let { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(it)) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDarkMode) Color(0x16FFFFFF) else Color(0x0A000000))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = (if (e.type == HisabType.DIYE) "दिए (Lent): " else "लिए (Took): ") + "₹${formatRupees(e.amount)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (e.isSettled) Color.Gray else (if (e.type == HisabType.DIYE) Color(0xFF10B981) else Color(0xFFEF4444)),
                                    textDecoration = if (e.isSettled) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (e.reason.isNotBlank()) {
                                    Text("कारण: ${e.reason}", fontSize = 11.sp, color = fg.copy(alpha = 0.8f))
                                }
                                Text(
                                    text = dt + (if (e.isSettled && retDt != null) " • Settled: $retDt" else ""),
                                    fontSize = 10.sp,
                                    color = sub
                                )
                            }
                            TextButton(onClick = { onToggleSettled(e) }) {
                                Text(
                                    text = if (e.isSettled) "Undo" else "लौटा दिया",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (e.isSettled) Color.Gray else Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHisabPlaceholder(isDarkMode: Boolean, message: String) {
    val sub = if (isDarkMode) Color.White.copy(alpha = 0.55f) else Color(0xFF64748B)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = sub,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HisabEntryDialog(
    initialEntry: HisabEntry?,
    defaultType: HisabType,
    prefillName: String,
    prefillPhone: String,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (HisabEntry) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initialEntry?.type ?: defaultType) }
    var personName by remember { mutableStateOf(initialEntry?.personName ?: prefillName) }
    var phoneNumber by remember { mutableStateOf(initialEntry?.phoneNumber ?: prefillPhone) }
    var amountText by remember {
        mutableStateOf(
            initialEntry?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
        )
    }
    var reason by remember { mutableStateOf(initialEntry?.reason.orEmpty()) }

    val fg = if (isDarkMode) Color.White else Color(0xFF111111)
    val greenColor = Color(0xFF10B981)
    val redColor = Color(0xFFEF4444)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialEntry == null) "नया हिसाब जोड़ें (Add Entry)" else "हिसाब अपडेट करें (Edit Entry)",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type Selector: उधार दिए vs उधार लिए
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDiye = type == HisabType.DIYE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDiye) greenColor else Color.Gray.copy(alpha = 0.15f))
                            .clickable { type = HisabType.DIYE }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "उधार दिए (Lent)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDiye) Color.White else fg
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isDiye) redColor else Color.Gray.copy(alpha = 0.15f))
                            .clickable { type = HisabType.LIYE }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "उधार लिए (Took)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isDiye) Color.White else fg
                        )
                    }
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(if (type == HisabType.DIYE) "किसको दिए? (Person Name)" else "किससे लिए? (Person Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("कितने रुपये? (Amount ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("क्यों दिए/लिए? (Reason / Note)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' } },
                    label = { Text("मोबाइल नंबर (Phone Number - Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = personName.trim()
                    val amt = amountText.trim().toDoubleOrNull() ?: 0.0
                    if (cleanName.isBlank()) {
                        Toast.makeText(context, "कृपया नाम लिखें (Please enter person name)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (amt <= 0.0) {
                        Toast.makeText(context, "कृपया सही रकम लिखें (Please enter valid amount)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val entry = HisabEntry(
                        id = initialEntry?.id ?: System.currentTimeMillis(),
                        personName = cleanName,
                        phoneNumber = phoneNumber.trim(),
                        amount = amt,
                        type = type,
                        reason = reason.trim(),
                        createdAt = initialEntry?.createdAt ?: System.currentTimeMillis(),
                        isSettled = initialEntry?.isSettled ?: false,
                        settledAt = initialEntry?.settledAt
                    )
                    onSave(entry)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Save Hisab", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatRupees(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%,d", value.toLong())
    } else {
        String.format(Locale.US, "%,.2f", value)
    }
}

private fun buildFullHisabReport(
    entries: List<HisabEntry>,
    totalPendingDiye: Double,
    totalPendingLiye: Double,
    highestUdharPerson: Triple<String, String, Double>?
): String = buildString {
    append("=== AU NOTES • HISAB KITAB (उधार खाता) ===\n")
    append("कुल लेने हैं (Pending Udhar Diye): ₹${formatRupees(totalPendingDiye)}\n")
    append("कुल देने हैं (Pending Udhar Liye): ₹${formatRupees(totalPendingLiye)}\n")
    if (highestUdharPerson != null && highestUdharPerson.third > 0) {
        append("सबसे ज्यादा उधार: ${highestUdharPerson.first} (₹${formatRupees(highestUdharPerson.third)})\n")
    }
    append("\n--- विस्तृत लेन-देन (TRANSACTIONS) ---\n")
    val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    entries.forEachIndexed { idx, e ->
        val tLabel = if (e.type == HisabType.DIYE) "उधार दिए (Lent)" else "उधार लिए (Borrowed)"
        val status = if (e.isSettled) " [SETTLED on ${e.settledAt?.let { fmt.format(Date(it)) } ?: ""}]" else " [PENDING]"
        append("${idx + 1}. ${e.personName} (${e.phoneNumber.ifBlank { "No phone" }}) — ₹${formatRupees(e.amount)} • $tLabel$status\n")
        if (e.reason.isNotBlank()) append("   कारण: ${e.reason}\n")
        append("   तारीख: ${fmt.format(Date(e.createdAt))}\n")
    }
}
