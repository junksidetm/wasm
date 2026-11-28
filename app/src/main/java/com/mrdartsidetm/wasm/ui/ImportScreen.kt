package com.mrdartsidetm.wasm.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.R
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.onboarding.ContainedExpressiveLoadingIndicator
import com.mrdartsidetm.wasm.ui.onboarding.OnboardingSvgPaths
import com.mrdartsidetm.wasm.ui.onboarding.OnboardingWhatsAppArrowNeon
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding535
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding875
import com.mrdartsidetm.wasm.util.DetectedArchivePlatform
import com.mrdartsidetm.wasm.util.ZipArchiveDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

private const val IMPORT_LAYERS_PATH =
    "M50.65 0.833 C52.32 0 54.28 0 55.95 0.833 L103.28 24.499 C105.28 25.502 106.55 27.55 106.55 29.791 C106.55 32.032 105.28 34.081 103.28 35.083 L90.97 41.237 L103.53 48.302 C105.44 49.375 106.6 51.414 106.55 53.603 C106.49 55.792 105.24 57.772 103.28 58.751 L90.97 64.904 L103.53 71.969 C105.44 73.042 106.6 75.081 106.55 77.27 C106.49 79.458 105.24 81.439 103.28 82.418 L55.95 106.084 C54.28 106.917 52.32 106.917 50.65 106.084 L3.32 82.418 C1.36 81.439 0.11 79.458 0.05 77.27 C0 75.081 1.16 73.042 3.07 71.969 L15.63 64.904 L3.32 58.751 C1.36 57.772 0.11 55.792 0.05 53.603 C0 51.414 1.16 49.375 3.07 48.302 L15.63 41.237 L3.32 35.083 C1.32 34.081 0.05 32.032 0.05 29.791 C0.05 27.55 1.32 25.502 3.32 24.499 L50.65 0.833 ZM28.4 71.293 L18.58 76.818 L53.3 94.177 L88.02 76.818 L78.2 71.293 L55.95 82.418 C54.28 83.251 52.32 83.251 50.65 82.418 L28.4 71.293 ZM55.95 58.75 L78.19 47.626 L88.02 53.152 L53.3 70.51 L18.58 53.152 L28.41 47.626 L50.65 58.75 C52.32 59.583 54.28 59.583 55.95 58.75 Z"

private const val IMPORT_META_LOGO_PATH =
    "M38.16 0.3 C42.5 0 45.95 2.517 48.36 5.676 C50.78 8.854 52.5 13.069 53.44 17.274 C54.38 21.478 54.62 25.939 53.86 29.675 C53.12 33.307 51.24 37.038 47.28 38.221 C43.51 39.349 40.12 37.746 37.54 35.571 C34.94 33.391 32.7 30.283 30.89 27.279 C29.47 24.923 28.26 22.522 27.31 20.473 C26.36 22.522 25.15 24.923 23.73 27.279 C21.92 30.283 19.68 33.391 17.08 35.571 C14.5 37.746 11.11 39.349 7.34 38.221 C3.38 37.038 1.5 33.307 0.76 29.675 C0 25.939 0.24 21.478 1.18 17.274 C2.12 13.069 3.84 8.854 6.26 5.676 C8.67 2.517 12.12 0 16.46 0.3 C21.17 0.627 24.53 4.29 26.47 7.06 C26.77 7.486 27.05 7.909 27.31 8.322 C27.57 7.909 27.85 7.486 28.15 7.06 C30.09 4.29 33.45 0.627 38.16 0.3 ZM16.08 5.722 C14.3 5.599 12.42 6.57 10.58 8.974 C8.76 11.361 7.31 14.787 6.48 18.464 C5.66 22.142 5.52 25.803 6.09 28.594 C6.68 31.491 7.82 32.694 8.89 33.015 C10.16 33.392 11.66 33.03 13.59 31.412 C15.5 29.798 17.38 27.283 19.07 24.476 C21.57 20.333 23.43 15.895 24.2 13.952 C23.77 13.042 23.03 11.605 22.03 10.184 C20.24 7.638 18.16 5.867 16.08 5.722 ZM38.54 5.722 C36.46 5.867 34.38 7.638 32.59 10.184 C31.59 11.605 30.85 13.042 30.42 13.952 C31.19 15.895 33.05 20.333 35.55 24.476 C37.24 27.283 39.12 29.798 41.03 31.412 C42.96 33.03 44.46 33.392 45.73 33.015 C46.8 32.694 47.94 31.491 48.53 28.594 C49.1 25.803 48.96 22.142 48.14 18.464 C47.31 14.787 45.86 11.361 44.04 8.974 C42.2 6.57 40.32 5.599 38.54 5.722 Z"

/**
 * Full-screen Material 3 Expressive Import Screen built from `assests/wireframes/main/Import Screen.svg`.
 * Auto-detects WhatsApp (.txt/.zip) and Instagram (.zip) exports cleanly offline.
 */
@Composable
fun ImportScreen(
    whatsappViewModel: ChatViewModel,
    instagramViewModel: InstagramViewModel,
    onImportComplete: (platform: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalWasmStrings.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val waImportState by whatsappViewModel.importUiState.collectAsStateWithLifecycle()
    val igImportState by instagramViewModel.importUiState.collectAsStateWithLifecycle()

    var awaitingPlatform by remember { mutableStateOf<String?>(null) }
    var isDetecting by remember { mutableStateOf(false) }

    val isImporting = isDetecting ||
        (awaitingPlatform == "whatsapp" && waImportState is ImportUiState.Loading) ||
        (awaitingPlatform == "instagram" && igImportState is ImportUiState.Loading)

    BackHandler(enabled = !isImporting, onBack = onBack)

    val activeState = when (awaitingPlatform) {
        "whatsapp" -> waImportState
        "instagram" -> igImportState
        else -> ImportUiState.Idle
    }

    LaunchedEffect(activeState, awaitingPlatform) {
        val current = awaitingPlatform ?: return@LaunchedEffect
        when (activeState) {
            is ImportUiState.Success -> {
                val msg = if (current == "whatsapp") strings.importedWhatsappSnackbar else strings.importedInstagramSnackbar
                if (current == "whatsapp") whatsappViewModel.dismissImportState() else instagramViewModel.dismissImportState()
                awaitingPlatform = null
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onImportComplete(current)
            }
            is ImportUiState.Error -> {
                val errMsg = activeState.message
                if (current == "whatsapp") whatsappViewModel.dismissImportState() else instagramViewModel.dismissImportState()
                awaitingPlatform = null
                snackbarHostState.showSnackbar(errMsg, duration = SnackbarDuration.Long)
            }
            else -> Unit
        }
    }

    val autoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri == null || isImporting) return@rememberLauncherForActivityResult
            scope.launch {
                isDetecting = true
                val detection = withContext(Dispatchers.IO) { inspectPickedArchive(context, uri) }
                isDetecting = false

                if (detection == null) {
                    snackbarHostState.showSnackbar(strings.importUnrecognizedFileError, duration = SnackbarDuration.Long)
                    return@launch
                }

                val (platform, fileName, cachedFile) = detection
                when (platform) {
                    DetectedArchivePlatform.WHATSAPP_TXT -> {
                        awaitingPlatform = "whatsapp"
                        whatsappViewModel.importChatUri(context.contentResolver, Uri.fromFile(cachedFile), fileName)
                    }
                    DetectedArchivePlatform.WHATSAPP_ZIP -> {
                        awaitingPlatform = "whatsapp"
                        whatsappViewModel.importZipUri(context.contentResolver, Uri.fromFile(cachedFile), fileName)
                    }
                    DetectedArchivePlatform.INSTAGRAM_ZIP -> {
                        awaitingPlatform = "instagram"
                        instagramViewModel.importZipUri(context.contentResolver, Uri.fromFile(cachedFile), fileName)
                    }
                    DetectedArchivePlatform.UNKNOWN -> {
                        snackbarHostState.showSnackbar(strings.importUnrecognizedFileError, duration = SnackbarDuration.Long)
                    }
                }
            }
        }
    )

    val launchAutoPicker = {
        if (!isImporting) {
            autoPickerLauncher.launch(
                arrayOf(
                    "application/zip",
                    "application/x-zip-compressed",
                    "application/octet-stream",
                    "text/plain",
                    "*/*"
                )
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar back affordance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { if (!isImporting) onBack() },
                    enabled = !isImporting,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = strings.back,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Top stacked layers mark (`Vector_30`) + "Choose Platform" heading matching Import Screen.svg
            val layersPath = remember {
                PathParser().parsePathString(IMPORT_LAYERS_PATH).toPath().apply {
                    fillType = PathFillType.EvenOdd
                }
            }
            val layersTint = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.size(68.dp)) {
                val s = min(size.width / 106.6f, size.height / 106.92f)
                val dx = (size.width - 106.6f * s) / 2f
                val dy = (size.height - 106.92f * s) / 2f
                withTransform({
                    translate(dx, dy)
                    scale(s, s, pivot = Offset.Zero)
                }) {
                    drawPath(path = layersPath, color = layersTint)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.choosePlatformTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = GoogleSansFlexOnboarding875,
                    fontWeight = FontWeight(875),
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Center interactive visual or loading indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isImporting,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ImportContentState"
                ) { loading ->
                    if (loading) {
                        val stepText = when {
                            isDetecting -> strings.importingWait
                            waImportState is ImportUiState.Loading -> (waImportState as ImportUiState.Loading).step
                            igImportState is ImportUiState.Loading -> (igImportState as ImportUiState.Loading).step
                            else -> strings.importingWait
                        }
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(200.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                ContainedExpressiveLoadingIndicator(
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(92.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = stepText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(26.dp)
                        ) {
                            // Dual platform icons (WhatsApp green tile & Instagram gradient tile)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = OnboardingWhatsAppArrowNeon,
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = launchAutoPicker)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_nav_whatsapp),
                                            contentDescription = strings.whatsapp,
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFF58529),
                                                    Color(0xFFDD2A7B),
                                                    Color(0xFF8134AF)
                                                )
                                            )
                                        )
                                        .clickable(onClick = launchAutoPicker),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_nav_instagram),
                                        contentDescription = strings.instagram,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Text(
                                text = strings.smartDetectionSubtitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = GoogleSansFlexOnboarding535,
                                    fontWeight = FontWeight(535)
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Exact Meta logo SVG path (`Union_17`) + explanation copy
                            val metaLogoPath = remember {
                                PathParser().parsePathString(IMPORT_META_LOGO_PATH).toPath().apply {
                                    fillType = PathFillType.EvenOdd
                                }
                            }
                            val metaLogoTint = MaterialTheme.colorScheme.onSurface
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Canvas(modifier = Modifier.size(width = 52.dp, height = 38.dp)) {
                                    val s = min(size.width / 54.62f, size.height / 39.35f)
                                    val dx = (size.width - 54.62f * s) / 2f
                                    val dy = (size.height - 39.35f * s) / 2f
                                    withTransform({
                                        translate(dx, dy)
                                        scale(s, s, pivot = Offset.Zero)
                                    }) {
                                        drawPath(path = metaLogoPath, color = metaLogoTint)
                                    }
                                }
                                Text(
                                    text = strings.autoCategorizeNotice,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = strings.importZipHint,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = GoogleSansFlexOnboarding535,
                    fontWeight = FontWeight(535)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            val importUnionPaths = remember {
                OnboardingSvgPaths.importUnionPaths.map { d ->
                    PathParser().parsePathString(d).toPath().apply {
                        fillType = PathFillType.EvenOdd
                    }
                }
            }
            Button(
                onClick = launchAutoPicker,
                enabled = !isImporting,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                val btnIconColor = LocalContentColor.current
                Canvas(modifier = Modifier.size(22.dp)) {
                    val s = min(
                        size.width / OnboardingSvgPaths.importUnionWidth,
                        size.height / OnboardingSvgPaths.importUnionHeight
                    )
                    val dx = (size.width - OnboardingSvgPaths.importUnionWidth * s) / 2f
                    val dy = (size.height - OnboardingSvgPaths.importUnionHeight * s) / 2f
                    withTransform({
                        translate(dx, dy)
                        scale(s, s, pivot = Offset.Zero)
                    }) {
                        importUnionPaths.forEach { p ->
                            drawPath(path = p, color = btnIconColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.importFile,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansFlexOnboarding535,
                        fontWeight = FontWeight(535),
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

private fun inspectPickedArchive(
    context: Context,
    uri: Uri
): Triple<DetectedArchivePlatform, String?, File>? {
    var fileName: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1) fileName = cursor.getString(idx)
        }
    }
    val tempDir = File(context.cacheDir, "home_imports").apply {
        mkdirs()
        listFiles()?.forEach { it.delete() }
    }
    val safeExt = if (fileName?.endsWith(".txt", ignoreCase = true) == true) ".txt" else ".zip"
    val tempFile = File(tempDir, "import_${System.currentTimeMillis()}$safeExt")

    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        val platform = ZipArchiveDetector.detectFromFile(tempFile, fileName)
        Triple(platform, fileName, tempFile)
    } catch (_: Exception) {
        tempFile.delete()
        null
    }
}
