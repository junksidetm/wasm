package com.mrdartsidetm.wasm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.R
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.localization.languageByCode
import com.mrdartsidetm.wasm.ui.onboarding.LanguageChangerScreen
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding875
import kotlinx.coroutines.launch

private enum class SettingsEntryKey {
    GENERAL,
    LANGUAGE,
    PRIVACY,
    STORAGE_DATA,
    ABOUT
}

private data class SettingsCategoryItem(
    val key: SettingsEntryKey,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

/**
 * Material 3 Expressive Settings Screen matching `assests/wireframes/main/Settings Screen.svg`:
 * - Bold dynamic-primary "Settings" header + Search Settings pill bar
 * - Connected M3 Expressive list rows: General, Language (opens main Language Changer),
 *   Privacy & Terms of Service, Storage & Data, About.
 */
@Composable
fun SettingsScreen(
    whatsappViewModel: ChatViewModel,
    instagramViewModel: InstagramViewModel,
    prefs: UserPreferencesRepository,
    selectedLanguageCode: String = "en",
    onReplayOnboarding: () -> Unit = {},
    onSubScreenChanged: (Boolean) -> Unit = {},
    onBackToHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalWasmStrings.current
    val scope = rememberCoroutineScope()
    val waConversations by whatsappViewModel.conversations.collectAsStateWithLifecycle()
    val waTotalMessages by whatsappViewModel.totalMessageCount.collectAsStateWithLifecycle()
    val igAccount by instagramViewModel.account.collectAsStateWithLifecycle()
    val igConversations by instagramViewModel.conversations.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showGeneralScreen by rememberSaveable { mutableStateOf(false) }
    var showLanguageChanger by rememberSaveable { mutableStateOf(false) }
    var showAboutScreen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showGeneralScreen, showLanguageChanger, showAboutScreen) {
        onSubScreenChanged(showGeneralScreen || showLanguageChanger || showAboutScreen)
    }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showStorageDataDialog by remember { mutableStateOf(false) }
    var showClearWhatsAppDialog by remember { mutableStateOf(false) }
    var showClearInstagramDialog by remember { mutableStateOf(false) }

    if (showClearWhatsAppDialog) {
        AlertDialog(
            onDismissRequest = { showClearWhatsAppDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear WhatsApp Data?") },
            text = { Text("This will permanently delete all saved WhatsApp conversations, messages, and attachments from local storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        whatsappViewModel.clearAllWhatsApp()
                        showClearWhatsAppDialog = false
                    }
                ) {
                    Text("Clear All WhatsApp", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearWhatsAppDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showClearInstagramDialog) {
        AlertDialog(
            onDismissRequest = { showClearInstagramDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Instagram Data?") },
            text = { Text("This will permanently remove all imported Instagram messages, conversations, and media files.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        instagramViewModel.clearInstagramData()
                        showClearInstagramDialog = false
                    }
                ) {
                    Text("Clear All Instagram", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearInstagramDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(strings.settingsPrivacyTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• Zero Network Permissions: No INTERNET permission is declared in AndroidManifest.xml.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• 100% Local Storage: All WhatsApp and Instagram chats, attachments, and media remain offline in your device Room database & internal files.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• License: Apache License 2.0 • Open Source.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showStorageDataDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDataDialog = false },
            icon = { Icon(Icons.Default.Storage, contentDescription = null) },
            title = { Text(strings.settingsStorageDataTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_whatsapp),
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.whatsappStorage, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            Text("${waConversations.size} chats • $waTotalMessages messages", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (waConversations.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                showStorageDataDialog = false
                                showClearWhatsAppDialog = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear All WhatsApp Data", maxLines = 1, softWrap = false)
                        }
                    }

                    Divider()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_instagram),
                            contentDescription = null,
                            tint = Color(0xFFE1306C),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.instagramStorage, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            Text(
                                text = if (igAccount != null) {
                                    "${igAccount!!.displayName.ifEmpty { igAccount!!.accountName }} • ${igConversations.size} chats"
                                } else {
                                    strings.noInstagramDataStored
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (igAccount != null) {
                        FilledTonalButton(
                            onClick = {
                                showStorageDataDialog = false
                                showClearInstagramDialog = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear All Instagram Data", maxLines = 1, softWrap = false)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageDataDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Opens main/General.svg screen
    if (showGeneralScreen) {
        GeneralScreen(
            whatsappViewModel = whatsappViewModel,
            prefs = prefs,
            onReplayOnboarding = onReplayOnboarding,
            onBack = { showGeneralScreen = false }
        )
        return
    }

    // Opens main/Language Changer.svg screen variant
    if (showLanguageChanger) {
        LanguageChangerScreen(
            selectedLanguageCode = selectedLanguageCode,
            isOnboardingStyle = false,
            onSelectLanguage = { lang ->
                scope.launch {
                    prefs.saveAppLanguage(lang.code)
                }
                showLanguageChanger = false
            },
            onBack = { showLanguageChanger = false }
        )
        return
    }

    if (showAboutScreen) {
        AboutScreen(
            onBack = { showAboutScreen = false }
        )
        return
    }

    val activeLanguage = languageByCode(selectedLanguageCode)
    val categories = remember(strings, activeLanguage, waConversations.size, igConversations.size) {
        listOf(
            SettingsCategoryItem(
                key = SettingsEntryKey.GENERAL,
                title = strings.settingsGeneralTitle,
                subtitle = strings.settingsGeneralSubtitle,
                icon = Icons.Default.Tune
            ),
            SettingsCategoryItem(
                key = SettingsEntryKey.LANGUAGE,
                title = strings.chooseLanguage.replace("\n", " "),
                subtitle = "${activeLanguage.flagEmoji} ${activeLanguage.nativeName} (${activeLanguage.code})",
                icon = Icons.Default.Language
            ),
            SettingsCategoryItem(
                key = SettingsEntryKey.PRIVACY,
                title = strings.settingsPrivacyTitle,
                subtitle = strings.settingsPrivacySubtitle,
                icon = Icons.Default.Shield
            ),
            SettingsCategoryItem(
                key = SettingsEntryKey.STORAGE_DATA,
                title = strings.settingsStorageDataTitle,
                subtitle = "${waConversations.size + igConversations.size} chats stored offline",
                icon = Icons.Default.Storage
            ),
            SettingsCategoryItem(
                key = SettingsEntryKey.ABOUT,
                title = strings.aboutTitle,
                subtitle = "Version 0.11.0 • Abhijeet Yadav",
                icon = Icons.Default.Info
            )
        )
    }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else {
            val q = searchQuery.trim().lowercase()
            categories.filter { item ->
                val matchesMain = item.title.lowercase().contains(q) || item.subtitle.lowercase().contains(q)
                val matchesGeneralKeywords = item.key == SettingsEntryKey.GENERAL &&
                    ("theme dark light dynamic color variant wallpaper font profile identity".contains(q))
                matchesMain || matchesGeneralKeywords
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Bold blue "Settings" header matching Settings Screen.svg
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = GoogleSansFlexOnboarding875,
                        fontWeight = FontWeight(875),
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Full-pill "Search Settings" bar matching Settings Screen.svg
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = strings.searchSettingsPlaceholder,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // M3 Expressive connected Settings list matching Settings Screen.svg
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(
                    items = filteredCategories,
                    key = { _, item -> item.key.name }
                ) { index, item ->
                    val outer = 20.dp
                    val inner = 6.dp
                    val shape = when {
                        filteredCategories.size <= 1 -> RoundedCornerShape(outer)
                        index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
                        index == filteredCategories.lastIndex -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
                        else -> RoundedCornerShape(inner)
                    }

                    Surface(
                        onClick = {
                            when (item.key) {
                                SettingsEntryKey.GENERAL -> showGeneralScreen = true
                                SettingsEntryKey.LANGUAGE -> showLanguageChanger = true
                                SettingsEntryKey.PRIVACY -> showPrivacyDialog = true
                                SettingsEntryKey.STORAGE_DATA -> showStorageDataDialog = true
                                SettingsEntryKey.ABOUT -> showAboutScreen = true
                            }
                        },
                        shape = shape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
