package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ApiConnectivityChecker
import com.example.data.ai.ProviderCheckResult
import com.example.data.preferences.AppPreferences
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.launch

private data class BuiltInProvider(
    val name: String,
    val subtitle: String,
    val defaultBaseUrl: String,
    val color: Color
)

private val BUILT_IN_PROVIDERS = listOf(
    BuiltInProvider("OpenAI", "GPT models via api.openai.com", "https://api.openai.com/v1", Color(0xFF10A37F)),
    BuiltInProvider("Claude", "Anthropic Claude models via api.anthropic.com", "https://api.anthropic.com/v1", Color(0xFFD97757)),
    BuiltInProvider("DeepSeek", "DeepSeek models via api.deepseek.com", "https://api.deepseek.com", Color(0xFF38BDF8)),
    BuiltInProvider("Groq", "Fast inference via api.groq.com", "https://api.groq.com/openai/v1", Color(0xFFF97316)),
    BuiltInProvider("OpenRouter", "Multi-provider router via openrouter.ai", "https://openrouter.ai/api/v1", Color(0xFF8B5CF6))
)

@Composable
fun ApiRoomScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val userApiKey by preferences.userApiKey.collectAsState()
    val selectedModel by preferences.selectedModel.collectAsState()
    val providerEntries by preferences.apiProviderEntries.collectAsState()
    val customProviders = remember(providerEntries) { providerEntries.values.filter { it.isCustom } }
    val activeProvider by preferences.activeProvider.collectAsState()
    val detectedModelsMap by preferences.detectedModels.collectAsState()

    val statusByProvider = remember { mutableStateMapOf<String, ProviderCheckResult>() }
    val checkingProvider = remember { mutableStateMapOf<String, Boolean>() }

    var showAddCustomDialog by remember { mutableStateOf(false) }

    val pageBg = if (isDarkMode) Color(0xFF0E0E10) else Color(0xFFFAF8F5)
    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val pillBg = if (isDarkMode) Color(0xFF232329) else Color(0xFFEFECE4)
    val textMain = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B)
    val textSub = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF636366)
    val warmAccent = Color(0xFFD97757)

    // Auto-run Model Detector on screen open if a Gemini key is already saved and no models are cached yet
    LaunchedEffect(Unit) {
        val initialKey = userApiKey.trim().ifBlank { preferences.getEffectiveApiKey().trim() }
        val existingGeminiModels = detectedModelsMap["Gemini"].orEmpty()
        if (initialKey.isNotBlank() && existingGeminiModels.isEmpty() && checkingProvider["Gemini"] != true) {
            checkingProvider["Gemini"] = true
            val res = ApiConnectivityChecker.check("Gemini", initialKey, "")
            checkingProvider["Gemini"] = false
            statusByProvider["Gemini"] = res
            if (res.ok && res.models.isNotEmpty()) {
                preferences.setDetectedModels("Gemini", res.models)
                if (selectedModel.removePrefix("models/") !in res.models) {
                    preferences.setSelectedModel(res.models.first())
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(pillBg)
                                .border(1.dp, cardBorder, CircleShape)
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "API Room",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMain
                            )
                            Text(
                                text = "Configure API keys & live Model Detector",
                                fontSize = 11.5.sp,
                                color = textSub
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showAddCustomDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = warmAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Custom API", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = textMain)
                    }
                }
            }

            // 1. Primary Google Gemini API Card + Live Model Detector
            item {
                val effectiveInitial = userApiKey.ifBlank { preferences.getEffectiveApiKey() }
                var keyInput by remember(userApiKey) { mutableStateOf(effectiveInitial) }
                var visible by remember { mutableStateOf(false) }
                val isChecking = checkingProvider["Gemini"] == true
                val checkResult = statusByProvider["Gemini"]
                val detectedGeminiModels = detectedModelsMap["Gemini"] ?: emptyList()
                val isGeminiActive = activeProvider.equals("Gemini", ignoreCase = true)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .border(
                            width = if (isGeminiActive) 1.5.dp else 1.dp,
                            color = if (isGeminiActive) warmAccent else cardBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(warmAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = warmAccent, modifier = Modifier.size(19.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Google Gemini API",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = textMain
                                )
                                Text(
                                    text = if (detectedGeminiModels.isNotEmpty()) {
                                        "${detectedGeminiModels.size} live models detected"
                                    } else {
                                        "Paste your key and tap Model Detector"
                                    },
                                    fontSize = 11.5.sp,
                                    color = if (detectedGeminiModels.isNotEmpty()) Color(0xFF22C55E) else textSub
                                )
                            }
                        }
                        StatusBadge(checkResult = checkResult, isChecking = isChecking, hasCachedModels = detectedGeminiModels.isNotEmpty())
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            preferences.setUseInbuiltApi(false)
                            preferences.setUserApiKey(it.trim())
                        },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIza...") },
                        singleLine = true,
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        clipboard.getText()?.text?.let { clip ->
                                            val clean = clip.trim()
                                            keyInput = clean
                                            preferences.setUseInbuiltApi(false)
                                            preferences.setUserApiKey(clean)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = warmAccent, modifier = Modifier.size(18.dp))
                                }
                                if (keyInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            keyInput = ""
                                            preferences.setUserApiKey("")
                                            preferences.setDetectedModels("Gemini", emptyList())
                                            statusByProvider.remove("Gemini")
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = textSub, modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = { visible = !visible }) {
                                    Icon(
                                        if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle visibility",
                                        tint = textSub,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = warmAccent,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val cleanKey = keyInput.trim().ifBlank { preferences.getEffectiveApiKey().trim() }
                            if (cleanKey.isBlank()) {
                                Toast.makeText(context, "Please enter your Gemini API key first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            keyInput = cleanKey
                            preferences.setActiveProvider("Gemini")
                            preferences.setUseInbuiltApi(false)
                            preferences.setUserApiKey(cleanKey)
                            checkingProvider["Gemini"] = true
                            scope.launch {
                                val res = ApiConnectivityChecker.check("Gemini", cleanKey, "")
                                checkingProvider["Gemini"] = false
                                statusByProvider["Gemini"] = res
                                if (res.ok && res.models.isNotEmpty()) {
                                    preferences.setDetectedModels("Gemini", res.models)
                                    val cleanSelected = selectedModel.removePrefix("models/")
                                    if (cleanSelected.isBlank() || cleanSelected !in res.models) {
                                        preferences.setSelectedModel(res.models.first())
                                    }
                                    Toast.makeText(context, "Detected ${res.models.size} live models", Toast.LENGTH_SHORT).show()
                                } else {
                                    preferences.setDetectedModels("Gemini", emptyList())
                                    Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isChecking,
                        colors = ButtonDefaults.buttonColors(containerColor = warmAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting Live Models...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Model Detector", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    checkResult?.let { res ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = res.message,
                            fontSize = 11.5.sp,
                            color = if (res.ok) Color(0xFF22C55E) else Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (detectedGeminiModels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = cardBorder)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DETECTED LIVE MODELS (${detectedGeminiModels.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = warmAccent
                            )
                            if (selectedModel.isNotBlank()) {
                                Text(
                                    text = "Active: ${selectedModel.removePrefix("models/")}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF22C55E)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            detectedGeminiModels.forEach { m ->
                                val isCurrent = isGeminiActive && selectedModel.removePrefix("models/").equals(m, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) warmAccent.copy(alpha = 0.14f) else pillBg)
                                        .border(
                                            width = 1.dp,
                                            color = if (isCurrent) warmAccent else cardBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            preferences.setActiveProvider("Gemini")
                                            preferences.setSelectedModel(m)
                                            Toast.makeText(context, "Active model: $m", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = m,
                                        fontSize = 12.5.sp,
                                        color = if (isCurrent) warmAccent else textMain,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = warmAccent,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Other Built-in Providers (OpenAI, Claude, DeepSeek, Groq, OpenRouter)
            items(BUILT_IN_PROVIDERS, key = { it.name }) { provider ->
                val entry = providerEntries[provider.name]
                ProviderCard(
                    name = provider.name,
                    subtitle = provider.subtitle,
                    accent = provider.color,
                    baseUrl = provider.defaultBaseUrl,
                    savedKey = entry?.apiKey ?: "",
                    isDarkMode = isDarkMode,
                    isActive = activeProvider.equals(provider.name, ignoreCase = true),
                    selectedModel = entry?.model?.ifBlank { selectedModel } ?: selectedModel,
                    detectedModels = detectedModelsMap[provider.name] ?: emptyList(),
                    isChecking = checkingProvider[provider.name] == true,
                    checkResult = statusByProvider[provider.name],
                    onSelectActive = {
                        preferences.setActiveProvider(provider.name)
                        Toast.makeText(context, "${provider.name} set as active engine", Toast.LENGTH_SHORT).show()
                    },
                    onSelectModel = { chosenModel ->
                        preferences.setActiveProvider(provider.name)
                        preferences.saveApiProvider(
                            name = provider.name,
                            apiKey = entry?.apiKey ?: "",
                            baseUrl = provider.defaultBaseUrl,
                            model = chosenModel,
                            isCustom = false
                        )
                        preferences.setSelectedModel(chosenModel)
                        Toast.makeText(context, "Active model: $chosenModel (${provider.name})", Toast.LENGTH_SHORT).show()
                    },
                    onSaveKey = { key ->
                        preferences.saveApiProvider(
                            name = provider.name,
                            apiKey = key.trim(),
                            baseUrl = provider.defaultBaseUrl,
                            model = entry?.model,
                            isCustom = false
                        )
                    },
                    onCheck = { key ->
                        val clean = key.trim()
                        if (clean.isBlank()) {
                            Toast.makeText(context, "Enter ${provider.name} API key first", Toast.LENGTH_SHORT).show()
                            return@ProviderCard
                        }
                        preferences.saveApiProvider(
                            name = provider.name,
                            apiKey = clean,
                            baseUrl = provider.defaultBaseUrl,
                            model = entry?.model,
                            isCustom = false
                        )
                        checkingProvider[provider.name] = true
                        scope.launch {
                            val res = ApiConnectivityChecker.check(provider.name, clean, provider.defaultBaseUrl)
                            checkingProvider[provider.name] = false
                            statusByProvider[provider.name] = res
                            if (res.ok && res.models.isNotEmpty()) {
                                preferences.setDetectedModels(provider.name, res.models)
                                preferences.setActiveProvider(provider.name)
                                val firstModel = res.models.first()
                                preferences.saveApiProvider(
                                    name = provider.name,
                                    apiKey = clean,
                                    baseUrl = provider.defaultBaseUrl,
                                    model = firstModel,
                                    isCustom = false
                                )
                                if (selectedModel !in res.models) {
                                    preferences.setSelectedModel(firstModel)
                                }
                            } else {
                                preferences.setDetectedModels(provider.name, emptyList())
                            }
                        }
                    },
                    onDelete = null
                )
            }

            // 3. Custom User-Added Providers
            items(customProviders, key = { "custom_${it.name}" }) { custom ->
                ProviderCard(
                    name = custom.name,
                    subtitle = custom.baseUrl,
                    accent = Color(0xFF06B6D4),
                    baseUrl = custom.baseUrl,
                    savedKey = custom.apiKey,
                    isDarkMode = isDarkMode,
                    isActive = activeProvider.equals(custom.name, ignoreCase = true),
                    selectedModel = custom.model.ifBlank { selectedModel },
                    detectedModels = detectedModelsMap[custom.name] ?: emptyList(),
                    isChecking = checkingProvider[custom.name] == true,
                    checkResult = statusByProvider[custom.name],
                    onSelectActive = {
                        preferences.setActiveProvider(custom.name)
                        Toast.makeText(context, "${custom.name} set as active engine", Toast.LENGTH_SHORT).show()
                    },
                    onSelectModel = { chosenModel ->
                        preferences.setActiveProvider(custom.name)
                        preferences.saveApiProvider(
                            name = custom.name,
                            apiKey = custom.apiKey,
                            baseUrl = custom.baseUrl,
                            model = chosenModel,
                            isCustom = true
                        )
                        preferences.setSelectedModel(chosenModel)
                    },
                    onSaveKey = { key ->
                        preferences.saveApiProvider(
                            name = custom.name,
                            apiKey = key.trim(),
                            baseUrl = custom.baseUrl,
                            model = custom.model,
                            isCustom = true
                        )
                    },
                    onCheck = { key ->
                        val clean = key.trim()
                        preferences.saveApiProvider(
                            name = custom.name,
                            apiKey = clean,
                            baseUrl = custom.baseUrl,
                            model = custom.model,
                            isCustom = true
                        )
                        checkingProvider[custom.name] = true
                        scope.launch {
                            val res = ApiConnectivityChecker.check(custom.name, clean, custom.baseUrl)
                            checkingProvider[custom.name] = false
                            statusByProvider[custom.name] = res
                            if (res.ok && res.models.isNotEmpty()) {
                                preferences.setDetectedModels(custom.name, res.models)
                                preferences.setActiveProvider(custom.name)
                                val firstModel = res.models.first()
                                preferences.saveApiProvider(
                                    name = custom.name,
                                    apiKey = clean,
                                    baseUrl = custom.baseUrl,
                                    model = firstModel,
                                    isCustom = true
                                )
                                if (selectedModel !in res.models) {
                                    preferences.setSelectedModel(firstModel)
                                }
                            }
                        }
                    },
                    onDelete = {
                        preferences.deleteApiProvider(custom.name)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }

    if (showAddCustomDialog) {
        var customName by remember { mutableStateOf("") }
        var customUrl by remember { mutableStateOf("https://") }
        var customKey by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = { Text("Add Custom OpenAI-Compatible API", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Provider Name (e.g. Mistral, Together)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("Base URL (e.g. https://api.mistral.ai/v1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customKey,
                        onValueChange = { customKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = warmAccent),
                    onClick = {
                        if (customName.isNotBlank() && customUrl.isNotBlank()) {
                            preferences.saveApiProvider(
                                name = customName.trim(),
                                apiKey = customKey.trim(),
                                baseUrl = customUrl.trim(),
                                model = "",
                                isCustom = true
                            )
                            showAddCustomDialog = false
                        }
                    }
                ) {
                    Text("Save Provider", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProviderCard(
    name: String,
    subtitle: String,
    accent: Color,
    baseUrl: String,
    savedKey: String,
    isDarkMode: Boolean,
    isActive: Boolean,
    selectedModel: String,
    detectedModels: List<String>,
    isChecking: Boolean,
    checkResult: ProviderCheckResult?,
    onSelectActive: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSaveKey: (String) -> Unit,
    onCheck: (String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val clipboard = LocalClipboardManager.current
    val cardBg = if (isDarkMode) Color(0xFF17171B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0xFF2C2C34) else Color(0xFFD6D3CB)
    val pillBg = if (isDarkMode) Color(0xFF232329) else Color(0xFFEFECE4)
    val textMain = if (isDarkMode) Color(0xFFF4F4F6) else Color(0xFF18181B)
    val textSub = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF636366)

    var keyInput by remember(savedKey) { mutableStateOf(savedKey) }
    var visible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) accent else cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent)
                Text(subtitle, fontSize = 11.sp, color = textSub)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(checkResult = checkResult, isChecking = isChecking, hasCachedModels = detectedModels.isNotEmpty())
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Provider", tint = Color(0xFFEF4444))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSelectActive,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isActive) "ACTIVE ENGINE FOR AU AGENT" else "SET AS ACTIVE ENGINE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isActive) accent else textMain
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = {
                keyInput = it
                onSaveKey(it)
            },
            label = { Text("$name API Key") },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            clipboard.getText()?.text?.let {
                                keyInput = it.trim()
                                onSaveKey(keyInput)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = accent)
                    }
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle visibility"
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = cardBorder,
                focusedTextColor = textMain,
                unfocusedTextColor = textMain
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { onCheck(keyInput) },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Detecting Models...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Model Detector", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        checkResult?.let { res ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = res.message,
                fontSize = 11.sp,
                color = if (res.ok) Color(0xFF22C55E) else Color(0xFFEF4444),
                fontWeight = FontWeight.SemiBold
            )
        }

        if (detectedModels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "DETECTED LIVE MODELS (${detectedModels.size})",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                detectedModels.forEach { m ->
                    val isCurrent = isActive && selectedModel.equals(m, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) accent.copy(alpha = 0.14f) else pillBg)
                            .border(1.dp, if (isCurrent) accent else cardBorder, RoundedCornerShape(10.dp))
                            .clickable { onSelectModel(m) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isCurrent,
                            onClick = { onSelectModel(m) },
                            colors = RadioButtonDefaults.colors(selectedColor = accent)
                        )
                        Text(
                            text = m,
                            fontSize = 12.sp,
                            color = textMain,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    checkResult: ProviderCheckResult?,
    isChecking: Boolean,
    hasCachedModels: Boolean = false
) {
    when {
        isChecking -> {
            CircularProgressIndicator(
                color = CrimsonPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        }
        checkResult == null && hasCachedModels -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF22C55E).copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("READY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
            }
        }
        checkResult == null -> {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.4f))
            )
        }
        checkResult.ok -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF22C55E).copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CONNECTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
            }
        }
        else -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("FAILED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            }
        }
    }
}
