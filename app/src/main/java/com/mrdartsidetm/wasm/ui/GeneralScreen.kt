package com.mrdartsidetm.wasm.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.theme.AppFontOption
import com.mrdartsidetm.wasm.ui.theme.ChatWallpaperPreset
import com.mrdartsidetm.wasm.ui.theme.ColorVariantOption
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding875
import com.mrdartsidetm.wasm.ui.theme.LocalIsDarkTheme
import com.mrdartsidetm.wasm.ui.theme.ThemeModeOption
import com.mrdartsidetm.wasm.ui.theme.getSelectedAppFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private enum class GeneralEntryKey {
    THEME,
    DYNAMIC_COLOR,
    WALLPAPER,
    FONT_SELECTION
}

private enum class GeneralExtraKey {
    PERSONALIZE,
    IDENTITY,
    REPLAY_ONBOARDING
}

private data class GeneralExtraItem(
    val key: GeneralExtraKey,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

/**
 * Material 3 Expressive General Settings Screen matching `assests/wireframes/main/General.svg`:
 * - Bold "General" heading + "Search Settings" pill bar
 * - Connected M3 Expressive card stack:
 *   1. Theme (3-segment connected button group: System Default | Light | Dark)
 *   2. Dynamic Color & Variants (Switch + Color Variant picker)
 *   3. Wallpaper (preset color swatches + custom photo picker)
 *   4. Font Selection (Google Sans Flex, SF Pro Display, System Default, Monospace)
 * - Secondary connected block below for Profile, Sender Identity & Replay Onboarding.
 */
@Composable
fun GeneralScreen(
    whatsappViewModel: ChatViewModel,
    prefs: UserPreferencesRepository,
    onReplayOnboarding: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalWasmStrings.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current

    BackHandler(onBack = onBack)

    val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeModeOption.SYSTEM.id)
    val dynamicColorEnabled by prefs.dynamicColorEnabled.collectAsStateWithLifecycle(initialValue = true)
    val colorVariant by prefs.colorVariant.collectAsStateWithLifecycle(initialValue = ColorVariantOption.DEFAULT.id)
    val chatWallpaper by prefs.chatWallpaper.collectAsStateWithLifecycle(initialValue = ChatWallpaperPreset.DEFAULT.id)
    val fontSelection by prefs.fontSelection.collectAsStateWithLifecycle(initialValue = AppFontOption.GOOGLE_SANS_FLEX.id)
    val currentUser by whatsappViewModel.currentUser.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showColorVariantDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showIdentityDialog by remember { mutableStateOf(false) }
    var newIdentityText by remember { mutableStateOf("") }
    var isPersonalizeOpen by rememberSaveable { mutableStateOf(false) }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let { selectedUri ->
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(selectedUri)?.use { input ->
                            val targetFile = File(context.filesDir, "chat_wallpaper.jpg")
                            targetFile.outputStream().use { output -> input.copyTo(output) }
                            prefs.saveChatWallpaperImagePath(targetFile.absolutePath)
                            prefs.saveChatWallpaper(ChatWallpaperPreset.CUSTOM.id)
                        }
                    } catch (_: Exception) {
                        // Ignore unreadable stream
                    }
                }
            }
        }
    )

    if (showColorVariantDialog) {
        val activeVariant = ColorVariantOption.fromId(colorVariant)
        AlertDialog(
            onDismissRequest = { showColorVariantDialog = false },
            icon = { Icon(Icons.Default.Palette, contentDescription = null) },
            title = { Text(strings.colorVariantPickerTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorVariantOption.values().forEach { variant ->
                        val selected = variant == activeVariant
                        Surface(
                            onClick = {
                                scope.launch { prefs.saveColorVariant(variant.id) }
                                showColorVariantDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(variant.previewPrimary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(variant.previewSecondary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(variant.previewTertiary)
                                    )
                                }
                                Text(
                                    text = variant.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorVariantDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showWallpaperDialog) {
        val activePreset = ChatWallpaperPreset.fromId(chatWallpaper)
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            icon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
            title = { Text(strings.wallpaperPickerTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChatWallpaperPreset.values()
                        .filter { it != ChatWallpaperPreset.CUSTOM }
                        .forEach { preset ->
                            val selected = preset == activePreset
                            val swatchColor = if (isDark) preset.darkColor else preset.lightColor
                            Surface(
                                onClick = {
                                    scope.launch { prefs.saveChatWallpaper(preset.id) }
                                    showWallpaperDialog = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = preset.displayName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                    FilledTonalButton(
                        onClick = {
                            showWallpaperDialog = false
                            wallpaperPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(strings.choosePhotoFromGallery, maxLines = 1)
                    }

                    if (activePreset != ChatWallpaperPreset.DEFAULT) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    prefs.saveChatWallpaper(ChatWallpaperPreset.DEFAULT.id)
                                    prefs.saveChatWallpaperImagePath(null)
                                }
                                showWallpaperDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.resetToDefault)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showFontDialog) {
        val activeFont = AppFontOption.fromId(fontSelection)
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            icon = { Icon(Icons.Default.TextFields, contentDescription = null) },
            title = { Text(strings.fontPickerTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFontOption.values().forEach { fontOption ->
                        val selected = fontOption == activeFont
                        val previewFamily = remember(fontOption) {
                            getSelectedAppFontFamily(context, fontOption.id)
                        }
                        Surface(
                            onClick = {
                                scope.launch { prefs.saveFontSelection(fontOption.id) }
                                showFontDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        scope.launch { prefs.saveFontSelection(fontOption.id) }
                                        showFontDialog = false
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fontOption.displayName,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = previewFamily,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "The quick brown fox jumps",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = previewFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showIdentityDialog) {
        AlertDialog(
            onDismissRequest = { showIdentityDialog = false },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            title = { Text("Set Default User Identity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.identityHelpText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newIdentityText,
                        onValueChange = { newIdentityText = it },
                        placeholder = { Text("e.g. Kajal Sinha") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newIdentityText.isNotBlank()) {
                            whatsappViewModel.setIdentity(newIdentityText.trim())
                        }
                        showIdentityDialog = false
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIdentityDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    val activeVariantName = ColorVariantOption.fromId(colorVariant).displayName
    val activeWallpaperName = ChatWallpaperPreset.fromId(chatWallpaper).displayName
    val activeFontName = AppFontOption.fromId(fontSelection).displayName

    val primaryEntries = remember(
        strings,
        searchQuery,
        activeVariantName,
        activeWallpaperName,
        activeFontName,
        dynamicColorEnabled
    ) {
        val all = listOf(
            GeneralEntryKey.THEME to "${strings.themeTitle} ${strings.themeSubtitle} ${strings.themeSystemDefault} ${strings.themeLight} ${strings.themeDark}",
            GeneralEntryKey.DYNAMIC_COLOR to "${strings.dynamicColorVariantsTitle} ${strings.dynamicColorVariantsSubtitle} $activeVariantName",
            GeneralEntryKey.WALLPAPER to "${strings.wallpaperTitle} ${strings.wallpaperSubtitle} $activeWallpaperName",
            GeneralEntryKey.FONT_SELECTION to "${strings.fontSelectionTitle} ${strings.fontSelectionSubtitle} $activeFontName"
        )
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) all.map { it.first }
        else all.filter { it.second.lowercase().contains(q) }.map { it.first }
    }

    val extraItems = remember(strings, currentUser, searchQuery) {
        val all = listOf(
            GeneralExtraItem(
                key = GeneralExtraKey.PERSONALIZE,
                title = strings.personalizeProfile,
                subtitle = strings.whatShouldWeCallYou,
                icon = Icons.Default.Person
            ),
            GeneralExtraItem(
                key = GeneralExtraKey.IDENTITY,
                title = strings.senderIdentityMe,
                subtitle = currentUser.ifEmpty { strings.notConfigured },
                icon = Icons.Default.Badge
            ),
            GeneralExtraItem(
                key = GeneralExtraKey.REPLAY_ONBOARDING,
                title = strings.replayOnboarding,
                subtitle = strings.languageSectionTitle,
                icon = Icons.Default.Replay
            )
        )
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) all
        else all.filter {
            it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q)
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

            // Top Header matching General.svg ("General" bold in onBackground) + clean back affordance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = strings.back,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = strings.settingsGeneralTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = GoogleSansFlexOnboarding875,
                        fontWeight = FontWeight(875),
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Full-pill "Search Settings" bar matching General.svg (rx=33)
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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Primary connected list from General.svg (outer=16dp, inner=4dp, 2dp spacing)
                itemsIndexed(
                    items = primaryEntries,
                    key = { _, key -> key.name }
                ) { index, entry ->
                    val shape = generalConnectedShape(index, primaryEntries.size)
                    when (entry) {
                        GeneralEntryKey.THEME -> {
                            GeneralThemeCard(
                                title = strings.themeTitle,
                                subtitle = strings.themeSubtitle,
                                selectedMode = ThemeModeOption.fromId(themeMode),
                                systemLabel = strings.themeSystemDefault,
                                lightLabel = strings.themeLight,
                                darkLabel = strings.themeDark,
                                shape = shape,
                                onSelectMode = { option ->
                                    scope.launch { prefs.saveThemeMode(option.id) }
                                }
                            )
                        }
                        GeneralEntryKey.DYNAMIC_COLOR -> {
                            Surface(
                                onClick = { showColorVariantDialog = true },
                                shape = shape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = strings.dynamicColorVariantsTitle,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 16.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (dynamicColorEnabled) {
                                                "${strings.dynamicColorVariantsSubtitle} • $activeVariantName"
                                            } else {
                                                activeVariantName
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Switch(
                                        checked = dynamicColorEnabled,
                                        onCheckedChange = { checked ->
                                            scope.launch { prefs.saveDynamicColorEnabled(checked) }
                                        }
                                    )
                                }
                            }
                        }
                        GeneralEntryKey.WALLPAPER -> {
                            GeneralSimpleRowCard(
                                icon = Icons.Default.Wallpaper,
                                title = strings.wallpaperTitle,
                                subtitle = "${strings.wallpaperSubtitle} • $activeWallpaperName",
                                shape = shape,
                                onClick = { showWallpaperDialog = true }
                            )
                        }
                        GeneralEntryKey.FONT_SELECTION -> {
                            GeneralSimpleRowCard(
                                icon = Icons.Default.TextFields,
                                title = strings.fontSelectionTitle,
                                subtitle = "${strings.fontSelectionSubtitle} • $activeFontName",
                                shape = shape,
                                onClick = { showFontDialog = true }
                            )
                        }
                    }
                }

                if (extraItems.isNotEmpty()) {
                    item(key = "general_extra_spacer") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    itemsIndexed(
                        items = extraItems,
                        key = { _, item -> item.key.name }
                    ) { index, item ->
                        val shape = generalConnectedShape(index, extraItems.size)
                        GeneralSimpleRowCard(
                            icon = item.icon,
                            title = item.title,
                            subtitle = item.subtitle,
                            shape = shape,
                            showChevron = true,
                            onClick = {
                                when (item.key) {
                                    GeneralExtraKey.PERSONALIZE -> isPersonalizeOpen = true
                                    GeneralExtraKey.IDENTITY -> {
                                        newIdentityText = currentUser
                                        showIdentityDialog = true
                                    }
                                    GeneralExtraKey.REPLAY_ONBOARDING -> {
                                        onBack()
                                        onReplayOnboarding()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isPersonalizeOpen,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(400)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(350))
        ) {
            PersonalizeScreen(
                prefs = prefs,
                onDismiss = { isPersonalizeOpen = false },
                onNavigateToSettings = { isPersonalizeOpen = false }
            )
        }
    }
}

@Composable
private fun GeneralThemeCard(
    title: String,
    subtitle: String,
    selectedMode: ThemeModeOption,
    systemLabel: String,
    lightLabel: String,
    darkLabel: String,
    shape: RoundedCornerShape,
    onSelectMode: (ThemeModeOption) -> Unit
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Connected 3-segment button group matching General.svg (`Connected button group_2`)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ThemeConnectedSegment(
                        label = systemLabel,
                        icon = Icons.Default.BrightnessAuto,
                        selected = selectedMode == ThemeModeOption.SYSTEM,
                        position = 0,
                        onClick = { onSelectMode(ThemeModeOption.SYSTEM) },
                        modifier = Modifier.weight(1.25f)
                    )
                    ThemeConnectedSegment(
                        label = lightLabel,
                        icon = Icons.Default.LightMode,
                        selected = selectedMode == ThemeModeOption.LIGHT,
                        position = 1,
                        onClick = { onSelectMode(ThemeModeOption.LIGHT) },
                        modifier = Modifier.weight(0.9f)
                    )
                    ThemeConnectedSegment(
                        label = darkLabel,
                        icon = Icons.Default.DarkMode,
                        selected = selectedMode == ThemeModeOption.DARK,
                        position = 2,
                        onClick = { onSelectMode(ThemeModeOption.DARK) },
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeConnectedSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    position: Int, // 0 = start, 1 = middle, 2 = end
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = tween(200),
        label = "ThemeSegBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        animationSpec = tween(200),
        label = "ThemeSegFg"
    )
    val startCorner by animateDpAsState(
        targetValue = when {
            selected -> 18.dp
            position == 0 -> 18.dp
            else -> 4.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ThemeSegStart"
    )
    val endCorner by animateDpAsState(
        targetValue = when {
            selected -> 18.dp
            position == 2 -> 18.dp
            else -> 4.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ThemeSegEnd"
    )

    val shape = RoundedCornerShape(
        topStart = startCorner,
        bottomStart = startCorner,
        topEnd = endCorner,
        bottomEnd = endCorner
    )

    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GeneralSimpleRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    shape: RoundedCornerShape,
    showChevron: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showChevron) {
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

private fun generalConnectedShape(index: Int, totalCount: Int): RoundedCornerShape {
    val outer = 16.dp
    val inner = 4.dp
    return when {
        totalCount <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(
            topStart = outer,
            topEnd = outer,
            bottomStart = inner,
            bottomEnd = inner
        )
        index == totalCount - 1 -> RoundedCornerShape(
            topStart = inner,
            topEnd = inner,
            bottomStart = outer,
            bottomEnd = outer
        )
        else -> RoundedCornerShape(inner)
    }
}
