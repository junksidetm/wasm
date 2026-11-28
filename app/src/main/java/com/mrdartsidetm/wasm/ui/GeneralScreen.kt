package com.mrdartsidetm.wasm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.mrdartsidetm.wasm.ui.theme.DynamicVariantOption
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding875
import com.mrdartsidetm.wasm.ui.theme.ThemeModeOption
import kotlinx.coroutines.launch

private enum class GeneralEntryKey {
    THEME,
    DYNAMIC_COLOR,
    WALLPAPER,
    FONT_SELECTION
}

/**
 * Material 3 Expressive General Settings Screen matching `assests/wireframes/main/General.svg`:
 * - Bold "General" heading + "Search Settings" pill bar
 * - Connected M3 Expressive card stack:
 *   1. Theme (3-segment connected button group: System Default | Light | Dark)
 *   2. Dynamic Color & Variants (opens ColorVariantsScreen; vertical divider before Switch)
 *   3. Wallpaper (opens 9:16 WallpaperScreen HUD)
 *   4. Font Selection (opens FontSelectionScreen with Google Sans Flex axes)
 */
@Composable
fun GeneralScreen(
    prefs: UserPreferencesRepository,
    onOpenColorVariants: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onOpenFontSelection: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalWasmStrings.current
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeModeOption.SYSTEM.id)
    val dynamicColorEnabled by prefs.dynamicColorEnabled.collectAsStateWithLifecycle(initialValue = true)
    val colorVariant by prefs.colorVariant.collectAsStateWithLifecycle(initialValue = ColorVariantOption.DEFAULT.id)
    val dynamicVariant by prefs.dynamicVariant.collectAsStateWithLifecycle(initialValue = DynamicVariantOption.TONAL_SPOT.id)
    val chatWallpaper by prefs.chatWallpaper.collectAsStateWithLifecycle(initialValue = ChatWallpaperPreset.DEFAULT.id)
    val fontSelection by prefs.fontSelection.collectAsStateWithLifecycle(initialValue = AppFontOption.GOOGLE_SANS_FLEX.id)

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val activeVariantName = if (dynamicColorEnabled) {
        DynamicVariantOption.fromId(dynamicVariant).displayName
    } else {
        ColorVariantOption.fromId(colorVariant).displayName
    }
    val activeWallpaperName = ChatWallpaperPreset.fromId(chatWallpaper).displayName
    val activeFontName = AppFontOption.fromId(fontSelection).displayName

    val primaryEntries = remember(
        strings,
        searchQuery,
        activeVariantName,
        activeWallpaperName,
        activeFontName
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

            // Search Pill Bar
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
                                onClick = onOpenColorVariants,
                                shape = shape,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                            text = activeVariantName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Separator between heading and Dynamic Color switch
                                    VerticalDivider(
                                        modifier = Modifier.height(28.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )

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
                                subtitle = activeWallpaperName,
                                shape = shape,
                                showChevron = true,
                                onClick = onOpenWallpaper
                            )
                        }
                        GeneralEntryKey.FONT_SELECTION -> {
                            GeneralSimpleRowCard(
                                icon = Icons.Default.TextFields,
                                title = strings.fontSelectionTitle,
                                subtitle = activeFontName,
                                shape = shape,
                                showChevron = true,
                                onClick = onOpenFontSelection
                            )
                        }
                    }
                }
            }
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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

                // Connected 3-segment button group with extra allocation for System Default so it never truncates
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
                        modifier = Modifier.weight(1.54f)
                    )
                    ThemeConnectedSegment(
                        label = lightLabel,
                        icon = Icons.Default.LightMode,
                        selected = selectedMode == ThemeModeOption.LIGHT,
                        position = 1,
                        onClick = { onSelectMode(ThemeModeOption.LIGHT) },
                        modifier = Modifier.weight(0.73f)
                    )
                    ThemeConnectedSegment(
                        label = darkLabel,
                        icon = Icons.Default.DarkMode,
                        selected = selectedMode == ThemeModeOption.DARK,
                        position = 2,
                        onClick = { onSelectMode(ThemeModeOption.DARK) },
                        modifier = Modifier.weight(0.73f)
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
    position: Int,
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
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                maxLines = 1,
                softWrap = false
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
