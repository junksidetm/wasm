package com.mrdartsidetm.wasm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.theme.AppFontOption
import com.mrdartsidetm.wasm.ui.theme.GoogleSansCodeFontFamily
import com.mrdartsidetm.wasm.ui.theme.SfProRegular
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Dedicated Font Selection & Google Sans Flex Variable Font Axes Tuner.
 * Sliders appear strictly when Google Sans Flex is active, matching Wallet-Flutter's
 * PersonalizationPage variable axis tuner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSelectionScreen(
    prefs: UserPreferencesRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val selectedFontId by prefs.fontSelection.collectAsStateWithLifecycle(initialValue = AppFontOption.GOOGLE_SANS_FLEX.id)
    val selectedFont = AppFontOption.fromId(selectedFontId)

    val grade by prefs.gsfGrade.collectAsStateWithLifecycle(initialValue = 0f)
    val weight by prefs.gsfWeight.collectAsStateWithLifecycle(initialValue = 400f)
    val width by prefs.gsfWidth.collectAsStateWithLifecycle(initialValue = 100f)
    val roundness by prefs.gsfRoundness.collectAsStateWithLifecycle(initialValue = 100f)
    val opticalSize by prefs.gsfOpticalSize.collectAsStateWithLifecycle(initialValue = 24f)
    val slant by prefs.gsfSlant.collectAsStateWithLifecycle(initialValue = 0f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Font Selection",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Live Typography Tester Card (inherits dynamic Typography from WasmTheme)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LIVE PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedFont.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "The quick brown fox jumps over the lazy dog. 12:45 PM — 100% offline archive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "Monospace code sample: `git commit -m \"feat: wasm\"`",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = GoogleSansCodeFontFamily),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Font Family Connected Card List
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    AppFontOption.values().forEachIndexed { idx, option ->
                        val isSelected = option == selectedFont
                        val previewFamily = when (option) {
                            AppFontOption.SF_PRO -> SfProRegular
                            AppFontOption.SYSTEM -> FontFamily.SansSerif
                            AppFontOption.MONOSPACE -> GoogleSansCodeFontFamily
                            AppFontOption.GOOGLE_SANS_FLEX -> MaterialTheme.typography.bodyLarge.fontFamily ?: FontFamily.SansSerif
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { prefs.saveFontSelection(option.id) }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FontDownload,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = previewFamily),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (idx < AppFontOption.values().lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }

            // Variable Font Axes Sliders — shown ONLY when Google Sans Flex is active
            AnimatedVisibility(
                visible = selectedFont == AppFontOption.GOOGLE_SANS_FLEX,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Google Sans Flex Axes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    scope.launch { prefs.resetGoogleSansFlexAxes() }
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset")
                            }
                        }

                        FontAxisSliderRow(
                            label = "Weight (wght)",
                            value = weight,
                            valueRange = 100f..1000f,
                            displayValue = "${weight.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(weight = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Width (wdth)",
                            value = width,
                            valueRange = 50f..150f,
                            displayValue = "${width.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(width = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Roundness (ROND)",
                            value = roundness,
                            valueRange = 0f..100f,
                            displayValue = "${roundness.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(roundness = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Grade (GRAD)",
                            value = grade,
                            valueRange = -200f..150f,
                            displayValue = "${grade.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(grade = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Optical Size (opsz)",
                            value = opticalSize,
                            valueRange = 8f..144f,
                            displayValue = "${opticalSize.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(opticalSize = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Slant (slnt)",
                            value = slant,
                            valueRange = -10f..0f,
                            displayValue = "${slant.roundToInt()}°",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(slant = v) } }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FontAxisSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = GoogleSansCodeFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
