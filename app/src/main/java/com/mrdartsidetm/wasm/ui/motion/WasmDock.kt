package com.mrdartsidetm.wasm.ui.motion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.mrdartsidetm.wasm.ui.surfaceContainerHigh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class WasmNavTab(val index: Int) {
    HOME(0),
    STORAGE(1),
    SETTINGS(2)
}

/**
 * Floating Material 3 Expressive dock modeled directly on Expressive Clock's `ClockDock`:
 * - Live fractional pill indicator read during placement (`pagePosition`), zero trailing spring lag.
 * - Per-tab icon animations triggered both on swipe arrival and repeat tap (respecting reduced motion).
 * - Detached contextual Import FAB on Home that scales out cleanly while centering the pill stage.
 */
@Composable
fun WasmDock(
    selectedTab: Int,
    homeLabel: String,
    storageLabel: String,
    settingsLabel: String,
    importLabel: String,
    onSelectTab: (Int) -> Unit,
    onOpenImport: () -> Unit,
    pagePosition: (() -> Float)? = null,
    showImportAction: Boolean = selectedTab == 0,
    modifier: Modifier = Modifier
) {
    val animatorsEnabled = rememberAnimatorsEnabled()
    val view = LocalView.current
    val density = LocalDensity.current

    val pillWidth = 224.dp
    val innerPad = 5.dp
    val tabWidth = (pillWidth - innerPad * 2) / 3f
    val tabWidthPx = with(density) { tabWidth.toPx() }
    val innerPadPx = with(density) { innerPad.toPx() }

    val fallbackOffset by animateFloatAsState(
        targetValue = selectedTab.coerceIn(0, 2).toFloat(),
        animationSpec = if (animatorsEnabled) {
            spring(stiffness = 380f, dampingRatio = 0.78f)
        } else {
            tween(durationMillis = 120)
        },
        label = "wasmDockFallbackOffset"
    )

    val addAlpha by animateFloatAsState(
        targetValue = if (showImportAction) 1f else 0f,
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        label = "wasmDockAddAlpha"
    )
    val addScale by animateFloatAsState(
        targetValue = if (showImportAction) 1f else 0.72f,
        animationSpec = if (animatorsEnabled) {
            spring(dampingRatio = 0.68f, stiffness = 420f)
        } else {
            tween(durationMillis = 160)
        },
        label = "wasmDockAddScale"
    )

    // Balance center alignment: shifts slightly right when the detached + button exits
    val dockShift by animateDpAsState(
        targetValue = if (showImportAction) 0.dp else 38.dp,
        animationSpec = if (animatorsEnabled) {
            spring(dampingRatio = 0.78f, stiffness = 360f)
        } else {
            tween(durationMillis = 180)
        },
        label = "wasmDockShift"
    )

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dockSurface = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.94f else 0.96f)
    val dockBorder = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    }
    val indicatorColor = MaterialTheme.colorScheme.secondaryContainer
    val activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(dockShift.roundToPx(), 0) }
                .width(pillWidth)
                .height(WasmMotionTokens.DockHeight)
                .clip(CircleShape)
                .background(dockSurface, CircleShape)
                .border(1.dp, dockBorder, CircleShape)
                .padding(innerPad)
        ) {
            // Active pill indicator driven during placement for 1:1 pager drag tracking
            Box(
                modifier = Modifier
                    .offset {
                        val pos = (pagePosition?.invoke() ?: fallbackOffset).coerceIn(0f, 2f)
                        IntOffset((pos * tabWidthPx).roundToInt(), 0)
                    }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(indicatorColor, CircleShape)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                WasmDockItem(
                    tab = WasmNavTab.HOME,
                    label = homeLabel,
                    selected = selectedTab == 0,
                    activeIcon = Icons.Filled.Home,
                    inactiveIcon = Icons.Outlined.Home,
                    activeTint = activeContentColor,
                    inactiveTint = inactiveContentColor,
                    animatorsEnabled = animatorsEnabled,
                    onClick = {
                        view.performWasmTickHaptic(true)
                        onSelectTab(0)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                WasmDockItem(
                    tab = WasmNavTab.STORAGE,
                    label = storageLabel,
                    selected = selectedTab == 1,
                    activeIcon = Icons.Filled.PieChart,
                    inactiveIcon = Icons.Outlined.PieChart,
                    activeTint = activeContentColor,
                    inactiveTint = inactiveContentColor,
                    animatorsEnabled = animatorsEnabled,
                    onClick = {
                        view.performWasmTickHaptic(true)
                        onSelectTab(1)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                WasmDockItem(
                    tab = WasmNavTab.SETTINGS,
                    label = settingsLabel,
                    selected = selectedTab == 2,
                    activeIcon = Icons.Filled.Settings,
                    inactiveIcon = Icons.Outlined.Settings,
                    activeTint = activeContentColor,
                    inactiveTint = inactiveContentColor,
                    animatorsEnabled = animatorsEnabled,
                    onClick = {
                        view.performWasmTickHaptic(true)
                        onSelectTab(2)
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        // Detached contextual Import / Add FAB on Home
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    alpha = addAlpha
                    scaleX = addScale
                    scaleY = addScale
                }
                .then(
                    if (showImportAction) {
                        Modifier.wasmElasticClickable(shape = CircleShape) {
                            view.performWasmTickHaptic(true)
                            onOpenImport()
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = importLabel,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun WasmDockItem(
    tab: WasmNavTab,
    label: String,
    selected: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    activeTint: Color,
    inactiveTint: Color,
    animatorsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconScale = remember { Animatable(1f) }
    val iconRotation = remember { Animatable(0f) }
    val iconTranslateY = remember { Animatable(0f) }

    var tapCount by remember { mutableIntStateOf(0) }
    var wasSelected by remember { mutableStateOf(selected) }

    suspend fun playTabAnimation() {
        when (tab) {
            WasmNavTab.HOME -> {
                iconScale.snapTo(1f)
                iconTranslateY.snapTo(0f)
                iconTranslateY.animateTo(-7f, spring(dampingRatio = 0.55f, stiffness = 560f))
                iconScale.animateTo(1.16f, spring(dampingRatio = 0.48f, stiffness = 520f))
                iconTranslateY.animateTo(0f, spring(dampingRatio = 0.58f, stiffness = 420f))
                iconScale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 420f))
            }
            WasmNavTab.STORAGE -> {
                iconRotation.snapTo(0f)
                iconScale.snapTo(0.88f)
                iconScale.animateTo(1.08f, spring(dampingRatio = 0.55f, stiffness = 460f))
                iconRotation.animateTo(360f, spring(dampingRatio = 0.66f, stiffness = 240f))
                iconScale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 420f))
            }
            WasmNavTab.SETTINGS -> {
                val target = iconRotation.value + 120f
                iconRotation.animateTo(
                    targetValue = target,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = 320f)
                )
            }
        }
    }

    LaunchedEffect(selected, animatorsEnabled) {
        val newlySelected = selected && !wasSelected
        wasSelected = selected
        if (newlySelected && animatorsEnabled) {
            playTabAnimation()
        }
    }

    LaunchedEffect(tapCount, animatorsEnabled) {
        if (tapCount > 0 && animatorsEnabled) {
            playTabAnimation()
        }
    }

    val tint by animateColorAsState(
        targetValue = if (selected) activeTint else inactiveTint,
        animationSpec = tween(durationMillis = 160),
        label = "wasmDockItemTint"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .semantics {
                this.selected = selected
                this.role = Role.Tab
                this.contentDescription = label
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (selected) tapCount++
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(25.dp)
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                    rotationZ = iconRotation.value
                    translationY = iconTranslateY.value
                }
        )
    }
}
