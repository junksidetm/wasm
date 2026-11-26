package com.mrdartsidetm.wasm.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.mrdartsidetm.wasm.ui.onboarding.OnboardingWhatsAppArrowNeon
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding535
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding875
import com.mrdartsidetm.wasm.util.DetectedArchivePlatform
import com.mrdartsidetm.wasm.util.ZipArchiveDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
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

            // Top stacked layers mark + "Choose Platform" heading matching Import Screen.svg
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

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
                                    shape = RoundedCornerShape(14.dp),
                                    color = OnboardingWhatsAppArrowNeon,
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(onClick = launchAutoPicker)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_nav_whatsapp),
                                            contentDescription = strings.whatsapp,
                                            tint = Color.White,
                                            modifier = Modifier.size(46.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(14.dp))
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
                                        modifier = Modifier.size(46.dp)
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

                            // Smart routing icon + explanation copy
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AllInclusive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(42.dp)
                                )
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

            Button(
                onClick = launchAutoPicker,
                enabled = !isImporting,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
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
