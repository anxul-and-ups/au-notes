package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ApiConnectivityChecker
import com.example.data.ai.ConnectivityResult
import com.example.data.preferences.AppPreferences
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.NeuIconButton
import com.example.ui.components.PinLockDialog
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.launch

private val BUILT_IN_PROVIDERS = listOf("Gemini", "OpenAI", "Anthropic", "DeepSeek")

@Composable
fun ApiRoomScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val apiRoomLocked by preferences.apiRoomLocked.collectAsState()
    val hasCustomPin by preferences.hasCustomPin.collectAsState()
    val storedPin by preferences.lockPin.collectAsState()
    val secQuestion by preferences.securityQuestion.collectAsState()
    val secAnswer by preferences.securityAnswer.collectAsState()

    var isUnlocked by remember { mutableStateOf(!apiRoomLocked) }
    var showAuthPrompt by remember { mutableStateOf(apiRoomLocked) }

    val useInbuiltApi by preferences.useInbuiltApi.collectAsState()
    val userApiKey by preferences.userApiKey.collectAsState()
    val selectedModel by preferences.selectedModel.collectAsState()
    val providerEntries by preferences.apiProviderEntries.collectAsState()
    val detectedModels by preferences.detectedModels.collectAsState()

    // Item 27: nothing below (keys, providers, models) is even composed
    // until the PIN is verified, so it can't leak while the dialog is up.
    if (showAuthPrompt && !isUnlocked) {
        GlassBackground(isDarkMode = isDarkMode) { Box(modifier = Modifier.fillMaxSize()) }
        PinLockDialog(
            correctPin = storedPin,
            title = "API Room Locked",
            subtitle = "Enter your PIN to view API keys and providers",
            securityQuestion = secQuestion,
            securityAnswer = secAnswer,
            isDarkMode = isDarkMode,
            requireSetup = !hasCustomPin,
            onDismiss = { onBack() },
            onUnlocked = { isUnlocked = true; showAuthPrompt = false },
            onSetupComplete = { pin, question, answer -> preferences.setSecurityDetails(pin, question, answer) },
            onPinReset = { newPin -> preferences.setLockPin(newPin) }
        )
        return
    }

    var expandedProvider by remember { mutableStateOf<String?>("Gemini") }
    var showAddProviderDialog by remember { mutableStateOf(false) }

    val activeProvider by preferences.activeProvider.collectAsState()

    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    isDarkMode = isDarkMode,
                    size = 38.dp,
                    iconSize = 18.dp,
                    tint = if (isDarkMode) Color.White else Color.Black,
                    onClick = onBack
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cable, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("API Room", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                }
                Spacer(modifier = Modifier.width(38.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    GeminiProviderCard(
                        isDarkMode = isDarkMode,
                        expanded = expandedProvider == "Gemini",
                        onToggleExpand = { expandedProvider = if (expandedProvider == "Gemini") null else "Gemini" },
                        useInbuiltApi = useInbuiltApi,
                        userApiKey = userApiKey,
                        selectedModel = selectedModel,
                        effectiveKey = preferences.getEffectiveApiKey(),
                        detectedModels = detectedModels["Gemini"] ?: emptyList(),
                        onModelsDetected = { preferences.setDetectedModels("Gemini", it) },
                        onSetUseInbuilt = preferences::setUseInbuiltApi,
                        onSetApiKey = preferences::setUserApiKey,
                        onSetModel = preferences::setSelectedModel,
                        isActive = activeProvider == "Gemini",
                        onSetActive = { preferences.setActiveProvider("Gemini") }
                    )
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }
}

@Composable
private fun GeminiProviderCard(
    isDarkMode: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    useInbuiltApi: Boolean,
    userApiKey: String,
    selectedModel: String,
    effectiveKey: String,
    detectedModels: List<String>,
    onModelsDetected: (List<String>) -> Unit,
    onSetUseInbuilt: (Boolean) -> Unit,
    onSetApiKey: (String) -> Unit,
    onSetModel: (String) -> Unit,
    isActive: Boolean,
    onSetActive: () -> Unit
) {
    var keyInput by remember(userApiKey) { mutableStateOf(userApiKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var detectorResult by remember { mutableStateOf<ConnectivityResult?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode, elevation = 2.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ProviderHeader("Setup Your Gemini API", if (userApiKey.isNotBlank()) "API Key Configured" else "Enter your Gemini API Key", isDarkMode, expanded, onToggleExpand, modifier = Modifier.weight(1f))
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it
                        detectorResult = null
                        onSetUseInbuilt(false)
                        onSetApiKey(it)
                    },
                    label = { Text("Gemini API Key (AIza...)") },
                    placeholder = { Text("Paste your Google AI Studio API key") },
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (keyInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    keyInput = ""
                                    detectorResult = null
                                    onSetApiKey("")
                                    onModelsDetected(emptyList())
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear key", tint = Color.Gray)
                                }
                            }
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.Gray)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CrimsonPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ModelDetectorRow(
                    isDetecting = isDetecting,
                    result = detectorResult,
                    models = detectedModels,
                    isRealList = detectedModels.isNotEmpty(),
                    selectedModel = selectedModel,
                    onSelectModel = onSetModel,
                    isDarkMode = isDarkMode,
                    onDetect = {
                        isDetecting = true
                        coroutineScope.launch {
                            val r = ApiConnectivityChecker.check("Gemini", keyInput.trim())
                            detectorResult = r
                            if (r.isActive && r.availableModels.isNotEmpty()) {
                                onSetActive()
                                onModelsDetected(r.availableModels)
                                if (selectedModel.removePrefix("models/") !in r.availableModels) {
                                    onSetModel(r.availableModels.first())
                                }
                            } else {
                                onModelsDetected(emptyList())
                            }
                            isDetecting = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    name: String,
    isDarkMode: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    savedKey: String,
    savedBaseUrl: String,
    savedModel: String,
    detectedModels: List<String>,
    onModelsDetected: (List<String>) -> Unit,
    onSelectModel: (String) -> Unit,
    needsBaseUrl: Boolean,
    onSave: (key: String, baseUrl: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    isActive: Boolean = false,
    onSetActive: () -> Unit = {}
) {
    var keyInput by remember(savedKey) { mutableStateOf(savedKey) }
    var urlInput by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var keyVisible by remember { mutableStateOf(false) }
    var detectorResult by remember { mutableStateOf<ConnectivityResult?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), isDarkMode = isDarkMode, elevation = 2.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ProviderHeader(name, if (isActive) "Active Engine • Configured" else if (savedKey.isNotBlank()) "Configured" else "Not configured", isDarkMode, expanded, onToggleExpand, modifier = Modifier.weight(1f))
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete provider", tint = Color(0xFFFF5252))
                    }
                }
            }
            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("● CURRENT ACTIVE AI ENGINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onSetActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color(0xFF2E7D32) else CrimsonPrimary.copy(alpha = 0.2f),
                        contentColor = if (isActive) Color.White else CrimsonPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isActive) "✓ ACTIVE ENGINE FOR AU AGENT" else "SET AS ACTIVE ENGINE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                if (needsBaseUrl) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it; onSave(keyInput, it) },
                        label = { Text("Base URL / endpoint") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CrimsonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it; onSave(it, urlInput) },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.Gray)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CrimsonPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
                ModelDetectorRow(
                    isDetecting = isDetecting,
                    result = detectorResult,
                    models = detectedModels,
                    isRealList = detectedModels.isNotEmpty(),
                    selectedModel = savedModel,
                    onSelectModel = onSelectModel,
                    isDarkMode = isDarkMode,
                    onDetect = {
                        isDetecting = true
                        coroutineScope.launch {
                            val r = ApiConnectivityChecker.check(name, keyInput, urlInput)
                            detectorResult = r
                            if (r.isActive && r.availableModels.isNotEmpty()) onModelsDetected(r.availableModels)
                            isDetecting = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProviderHeader(
    name: String,
    subtitle: String,
    isDarkMode: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onToggleExpand),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = CrimsonPrimary
        )
    }
}

@Composable
private fun ModelDetectorRow(
    isDetecting: Boolean,
    result: ConnectivityResult?,
    models: List<String>,
    isRealList: Boolean,
    selectedModel: String,
    onSelectModel: (String) -> Unit,
    isDarkMode: Boolean,
    onDetect: () -> Unit
) {
    Spacer(modifier = Modifier.height(10.dp))
    Button(
        onClick = onDetect,
        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
        modifier = Modifier.fillMaxWidth(),
        enabled = !isDetecting
    ) {
        if (isDetecting) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text("Model Detector")
        }
    }
    result?.let { r ->
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (r.isActive) Color(0x1F4CAF50) else Color(0x1FFF5252),
                    RoundedCornerShape(10.dp)
                )
                .padding(10.dp)
        ) {
            Text(
                text = if (r.isActive) "● ACTIVE" else "● INACTIVE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (r.isActive) Color(0xFF4CAF50) else Color(0xFFFF5252)
            )
            Text(r.statusMessage, fontSize = 11.sp, color = Color.Gray)
            if (r.isActive) {
                Text("${r.availableModels.size} models found", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
    if (models.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isRealList) "Detected models — tap to select" else "Default models (run Model Detector for your real list)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color.Black
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState())
        ) {
            models.forEach { m ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectModel(m) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedModel == m,
                        onClick = { onSelectModel(m) },
                        colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                    )
                    Text(m, fontSize = 12.sp, color = if (isDarkMode) Color.White else Color.Black)
                }
            }
        }
    }
}
