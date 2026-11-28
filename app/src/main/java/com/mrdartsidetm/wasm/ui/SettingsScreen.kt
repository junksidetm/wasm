package com.mrdartsidetm.wasm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.motion.WasmMotionTokens
import com.mrdartsidetm.wasm.ui.motion.rememberAnimatorsEnabled
import com.mrdartsidetm.wasm.ui.motion.rememberLazyListHeaderFalloff
import com.mrdartsidetm.wasm.ui.motion.wasmFeatheredVerticalViewport
import com.mrdartsidetm.wasm.ui.motion.wasmLuminousElasticClickable
import com.mrdartsidetm.wasm.ui.onboarding.LanguageChangerScreen
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding875
import kotlinx.coroutines.launch

private enum class SettingsRoute(val depth: Int) {
    ROOT(0),
    GENERAL(1),
    LANGUAGE(1),
    STORAGE_DATA(1),
    ABOUT(1),
    COLOR_VARIANTS(2),
    WALLPAPER(2),
    FONT_SELECTION(2)
}

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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    whatsappViewModel: ChatViewModel,
    instagramViewModel: InstagramViewModel,
    prefs: UserPreferencesRepository,
    selectedLanguageCode: String = "en",
    onReplayOnboarding: () -> Unit = {},
    onSubScreenChanged: (Boolean) -> Unit = {},
    onConfirmation: (String) -> Unit = {},
    onBackToHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalWasmStrings.current
    val scope = rememberCoroutineScope()
    val animatorsEnabled = rememberAnimatorsEnabled()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showGeneralScreen by rememberSaveable { mutableStateOf(false) }
    var showColorVariantsScreen by rememberSaveable { mutableStateOf(false) }
    var showWallpaperScreen by rememberSaveable { mutableStateOf(false) }
    var showFontSelectionScreen by rememberSaveable { mutableStateOf(false) }
    var showLanguageChanger by rememberSaveable { mutableStateOf(false) }
    var showStorageDataScreen by rememberSaveable { mutableStateOf(false) }
    var showAboutScreen by rememberSaveable { mutableStateOf(false) }

    val activeRoute = when {
        showColorVariantsScreen -> SettingsRoute.COLOR_VARIANTS
        showWallpaperScreen -> SettingsRoute.WALLPAPER
        showFontSelectionScreen -> SettingsRoute.FONT_SELECTION
        showGeneralScreen -> SettingsRoute.GENERAL
        showLanguageChanger -> SettingsRoute.LANGUAGE
        showStorageDataScreen -> SettingsRoute.STORAGE_DATA
        showAboutScreen -> SettingsRoute.ABOUT
        else -> SettingsRoute.ROOT
    }

    LaunchedEffect(activeRoute) {
        onSubScreenChanged(activeRoute != SettingsRoute.ROOT)
    }

    var showPrivacyDialog by remember { mutableStateOf(false) }

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

    AnimatedContent(
        targetState = activeRoute,
        transitionSpec = {
            val forward = targetState.depth >= initialState.depth
            if (!animatorsEnabled) {
                fadeIn(tween(150)) togetherWith fadeOut(tween(120))
            } else if (forward) {
                (slideInHorizontally(
                    animationSpec = spring(stiffness = 380f, dampingRatio = 0.86f),
                    initialOffsetX = { width -> width }
                ) + fadeIn(tween(200, easing = FastOutSlowInEasing))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        targetOffsetX = { width -> -width / 5 }
                    ) + scaleOut(targetScale = 0.96f, animationSpec = tween(240)) + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(
                    animationSpec = spring(stiffness = 380f, dampingRatio = 0.86f),
                    initialOffsetX = { width -> -width / 5 }
                ) + scaleIn(initialScale = 0.96f, animationSpec = spring(stiffness = 380f, dampingRatio = 0.86f)) +
                    fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = spring(stiffness = 400f, dampingRatio = 0.88f),
                        targetOffsetX = { width -> width }
                    ) + fadeOut(tween(180, easing = FastOutSlowInEasing)))
            }
        },
        label = "SettingsHierarchyTransition",
        modifier = modifier.fillMaxSize()
    ) { route ->
        when (route) {
            SettingsRoute.COLOR_VARIANTS -> ColorVariantsScreen(
                prefs = prefs,
                onBack = { showColorVariantsScreen = false }
            )
            SettingsRoute.WALLPAPER -> WallpaperScreen(
                prefs = prefs,
                onBack = { showWallpaperScreen = false }
            )
            SettingsRoute.FONT_SELECTION -> FontSelectionScreen(
                prefs = prefs,
                onBack = { showFontSelectionScreen = false }
            )
            SettingsRoute.GENERAL -> GeneralScreen(
                prefs = prefs,
                onOpenColorVariants = { showColorVariantsScreen = true },
                onOpenWallpaper = { showWallpaperScreen = true },
                onOpenFontSelection = { showFontSelectionScreen = true },
                onBack = { showGeneralScreen = false }
            )
            SettingsRoute.LANGUAGE -> LanguageChangerScreen(
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
            SettingsRoute.STORAGE_DATA -> StorageDataScreen(
                whatsappViewModel = whatsappViewModel,
                instagramViewModel = instagramViewModel,
                onConfirmation = onConfirmation,
                onBack = { showStorageDataScreen = false }
            )
            SettingsRoute.ABOUT -> AboutScreen(
                onBack = { showAboutScreen = false }
            )
            SettingsRoute.ROOT -> {
                BackHandler(enabled = onBackToHome != null) {
                    onBackToHome?.invoke()
                }

                val categories = remember(strings) {
                    listOf(
                        SettingsCategoryItem(
                            key = SettingsEntryKey.GENERAL,
                            title = strings.settingsGeneralTitle,
                            subtitle = "",
                            icon = Icons.Default.Tune
                        ),
                        SettingsCategoryItem(
                            key = SettingsEntryKey.LANGUAGE,
                            title = strings.chooseLanguage.replace("\n", " "),
                            subtitle = "",
                            icon = Icons.Default.Language
                        ),
                        SettingsCategoryItem(
                            key = SettingsEntryKey.PRIVACY,
                            title = strings.settingsPrivacyTitle,
                            subtitle = "",
                            icon = Icons.Default.Shield
                        ),
                        SettingsCategoryItem(
                            key = SettingsEntryKey.STORAGE_DATA,
                            title = strings.settingsStorageDataTitle,
                            subtitle = "",
                            icon = Icons.Default.Storage
                        ),
                        SettingsCategoryItem(
                            key = SettingsEntryKey.ABOUT,
                            title = strings.aboutTitle,
                            subtitle = "",
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

                val listState = rememberLazyListState()
                val headerFalloff by rememberLazyListHeaderFalloff(listState)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .graphicsLayer {
                                    alpha = 1f - 0.14f * headerFalloff
                                    translationY = -4.dp.toPx() * headerFalloff
                                },
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
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .wasmFeatheredVerticalViewport(
                                    active = listState.canScrollForward || listState.canScrollBackward,
                                    topFadeFraction = 0.025f,
                                    bottomFadeFraction = 0.06f
                                ),
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
                                    shape = shape,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItemPlacement(WasmMotionTokens.SpringListPlacement)
                                        .wasmLuminousElasticClickable(
                                            shape = shape,
                                            maxStretchPercent = 0.025f,
                                            pressedScale = 0.975f,
                                            onClick = {
                                                when (item.key) {
                                                    SettingsEntryKey.GENERAL -> showGeneralScreen = true
                                                    SettingsEntryKey.LANGUAGE -> showLanguageChanger = true
                                                    SettingsEntryKey.PRIVACY -> showPrivacyDialog = true
                                                    SettingsEntryKey.STORAGE_DATA -> showStorageDataScreen = true
                                                    SettingsEntryKey.ABOUT -> showAboutScreen = true
                                                }
                                            }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 16.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
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
        }
    }
}
