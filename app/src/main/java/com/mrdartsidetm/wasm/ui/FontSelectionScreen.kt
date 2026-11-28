package com.mrdartsidetm.wasm.ui

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.components.WasmExpressiveSlider
import com.mrdartsidetm.wasm.ui.motion.WasmMotionTokens
import com.mrdartsidetm.wasm.ui.motion.wasmFeatheredVerticalViewport
import com.mrdartsidetm.wasm.ui.motion.wasmLuminousElasticClickable
import com.mrdartsidetm.wasm.ui.theme.AppFontOption
import com.mrdartsidetm.wasm.ui.theme.EmojiFontManager
import com.mrdartsidetm.wasm.ui.theme.EmojiPackOption
import com.mrdartsidetm.wasm.ui.theme.LocalWasmMonoFontFamily
import com.mrdartsidetm.wasm.ui.theme.SfProRegular
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    BackHandler(onBack = onBack)
    val selectedFontId by prefs.fontSelection.collectAsStateWithLifecycle(initialValue = AppFontOption.GOOGLE_SANS_FLEX.id)
    val selectedFont = AppFontOption.fromId(selectedFontId)
    val selectedEmojiId by prefs.emojiFont.collectAsStateWithLifecycle(initialValue = EmojiPackOption.DEFAULT.id)
    val selectedEmoji = EmojiPackOption.fromId(selectedEmojiId)
    val emojiRevision by prefs.emojiRevision.collectAsStateWithLifecycle(initialValue = 0)
    val monoFontFamily = LocalWasmMonoFontFamily.current

    var pendingUpdatePack by remember { mutableStateOf<EmojiPackOption?>(null) }

    val emojiFontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val pack = pendingUpdatePack ?: EmojiPackOption.WHATSAPP
        pendingUpdatePack = null
        if (uri != null) {
            scope.launch {
                val fileName = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                        }
                    }.getOrNull()
                }
                val result = withContext(Dispatchers.IO) {
                    EmojiFontManager.installUpdatedEmojiFont(context, pack, uri, fileName)
                }
                result.onSuccess { installed ->
                    prefs.recordEmojiOverride(installed.pack.id, installed.label)
                    snackbarHostState.showSnackbar("Updated ${installed.pack.displayName} (${installed.label})")
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(err.message ?: "Failed to update emoji font")
                }
            }
        }
    }

    val grade by prefs.gsfGrade.collectAsStateWithLifecycle(initialValue = 0f)
    val weight by prefs.gsfWeight.collectAsStateWithLifecycle(initialValue = 400f)
    val width by prefs.gsfWidth.collectAsStateWithLifecycle(initialValue = 100f)
    val roundness by prefs.gsfRoundness.collectAsStateWithLifecycle(initialValue = 100f)
    val opticalSize by prefs.gsfOpticalSize.collectAsStateWithLifecycle(initialValue = 24f)
    val slant by prefs.gsfSlant.collectAsStateWithLifecycle(initialValue = 0f)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .wasmFeatheredVerticalViewport(topFadeFraction = 0.02f, bottomFadeFraction = 0.05f)
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
                    AnimatedContent(
                        targetState = selectedFont.displayName,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                        label = "fontPreviewName"
                    ) { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "The quick brown fox jumps over the lazy dog. 12:45 PM — 100% offline archive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "\uD83D\uDE02 \uD83D\uDE0D \uD83D\uDCA6 \uD83D\uDE0E \uD83C\uDF89 \u2764\uFE0F \uD83D\uDD25 \uD83D\uDE4F",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = selectedEmoji.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "Monospace code sample: `git commit -m \"feat: wasm\"`",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFontFamily),
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
                            AppFontOption.MONOSPACE -> monoFontFamily
                            AppFontOption.GOOGLE_SANS_FLEX -> MaterialTheme.typography.bodyLarge.fontFamily ?: FontFamily.SansSerif
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wasmLuminousElasticClickable(
                                    shape = RoundedCornerShape(14.dp),
                                    maxStretchPercent = 0.025f,
                                    pressedScale = 0.98f,
                                    onClick = { scope.launch { prefs.saveFontSelection(option.id) } }
                                )
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

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = scaleIn(initialScale = 0.55f, animationSpec = WasmMotionTokens.SpringSelectionTick) + fadeIn(tween(110)),
                                exit = scaleOut(targetScale = 0.7f, animationSpec = tween(140)) + fadeOut(tween(110))
                            ) {
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
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }

            // Emoji Style Connected Card List (WhatsApp default, Facebook switchable, & updatable .ttf)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emoji Style",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Default is WhatsApp; switch packs or update with any .ttf",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    EmojiPackOption.values().forEachIndexed { idx, pack ->
                        val isSelected = pack == selectedEmoji
                        val customLabel by prefs.emojiOverrideLabelFlow(pack.id)
                            .collectAsStateWithLifecycle(initialValue = null)
                        val hasOverride = remember(pack.id, emojiRevision) {
                            EmojiFontManager.hasOverride(context, pack.id)
                        }
                        val versionBadge = when {
                            hasOverride && !customLabel.isNullOrBlank() -> "Updated: $customLabel"
                            hasOverride -> "Updated .ttf"
                            pack == EmojiPackOption.DEFAULT -> "v${pack.bundledVersion} · Default"
                            pack.bundledAssetPath != null -> "v${pack.bundledVersion}"
                            else -> pack.bundledVersion
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wasmLuminousElasticClickable(
                                    shape = RoundedCornerShape(14.dp),
                                    maxStretchPercent = 0.025f,
                                    pressedScale = 0.98f,
                                    onClick = { scope.launch { prefs.saveEmojiFont(pack.id) } }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEmotions,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pack.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = versionBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasOverride) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (pack != EmojiPackOption.SYSTEM) {
                                if (hasOverride) {
                                    IconButton(
                                        onClick = {
                                            EmojiFontManager.clearPackOverride(context, pack)
                                            scope.launch {
                                                prefs.bumpEmojiRevisionOnly(pack.id)
                                                snackbarHostState.showSnackbar("Restored bundled ${pack.displayName}")
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Restore bundled ${pack.displayName}",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                FilledTonalIconButton(
                                    onClick = {
                                        pendingUpdatePack = pack
                                        emojiFontPickerLauncher.launch(arrayOf("*/*"))
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = "Update ${pack.displayName} .ttf",
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = scaleIn(initialScale = 0.55f, animationSpec = WasmMotionTokens.SpringSelectionTick) + fadeIn(tween(110)),
                                exit = scaleOut(targetScale = 0.7f, animationSpec = tween(140)) + fadeOut(tween(110))
                            ) {
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
                        if (idx < EmojiPackOption.values().lastIndex) {
                            Divider(
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
                enter = expandVertically(animationSpec = spring(stiffness = 450f, dampingRatio = 0.80f)) + fadeIn(tween(180)),
                exit = shrinkVertically(animationSpec = spring(stiffness = 450f, dampingRatio = 0.80f)) + fadeOut(tween(150))
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
                            anchorValue = 400f,
                            displayValue = "${weight.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(weight = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Width (wdth)",
                            value = width,
                            valueRange = 50f..150f,
                            anchorValue = 100f,
                            displayValue = "${width.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(width = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Roundness (ROND)",
                            value = roundness,
                            valueRange = 0f..100f,
                            anchorValue = 100f,
                            displayValue = "${roundness.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(roundness = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Grade (GRAD)",
                            value = grade,
                            valueRange = -200f..150f,
                            anchorValue = 0f,
                            displayValue = "${grade.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(grade = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Optical Size (opsz)",
                            value = opticalSize,
                            valueRange = 8f..144f,
                            anchorValue = 24f,
                            displayValue = "${opticalSize.roundToInt()}",
                            onValueChange = { v -> scope.launch { prefs.saveGoogleSansFlexAxes(opticalSize = v) } }
                        )
                        FontAxisSliderRow(
                            label = "Slant (slnt)",
                            value = slant,
                            valueRange = -10f..0f,
                            anchorValue = 0f,
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
    anchorValue: Float? = null,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    val monoFontFamily = LocalWasmMonoFontFamily.current
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
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        WasmExpressiveSlider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            anchorValue = anchorValue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
