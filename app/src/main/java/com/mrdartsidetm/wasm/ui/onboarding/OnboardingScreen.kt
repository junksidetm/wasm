package com.mrdartsidetm.wasm.ui.onboarding

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.ChatViewModel
import com.mrdartsidetm.wasm.ui.ImportUiState
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.localization.languageByCode
import com.mrdartsidetm.wasm.util.DetectedArchivePlatform
import com.mrdartsidetm.wasm.util.ZipArchiveDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

enum class OnboardingStep {
    SCREEN_1,
    SCREEN_2,
    SCREEN_3,
    SCREEN_4_WHATSAPP,
    SCREEN_4_INSTAGRAM,
    SCREEN_5
}

private enum class PlatformTransitionMode {
    NONE,
    WHATSAPP_ARROWS,
    INSTAGRAM_FLOWER
}

private data class ElementPlacement(
    val xFraction: Float,
    val yFraction: Float,
    val sizeFraction: Float,
    val visible: Boolean = true
)

/**
 * Full 5-step onboarding experience faithful to the Figma Android Compact SVGs,
 * responsive across all Android screen sizes using section- and group-relative coordinates,
 * continuous axis rotation for "Move" and "Outer-Yellow" shapes, seamless shared-element motion,
 * expressive WhatsApp Arrow & Instagram Flower transitions, M3 Expressive shape-morphing loading
 * indicator, auto-detecting chat import with Snackbar confirmation, and live 11-language switching.
 */
@Composable
fun OnboardingFlow(
    whatsappViewModel: ChatViewModel,
    instagramViewModel: InstagramViewModel,
    prefs: UserPreferencesRepository,
    selectedLanguageCode: String,
    onFinishOnboarding: (importedPlatform: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalWasmStrings.current

    var currentStep by rememberSaveable { mutableStateOf(OnboardingStep.SCREEN_1) }
    var showLanguageChanger by rememberSaveable { mutableStateOf(false) }
    var transitionMode by remember { mutableStateOf(PlatformTransitionMode.NONE) }
    var lastImportedPlatform by rememberSaveable { mutableStateOf<String?>(null) }
    var awaitingImportPlatform by remember { mutableStateOf<String?>(null) }
    var isDetectingFile by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val waImportState by whatsappViewModel.importUiState.collectAsStateWithLifecycle()
    val igImportState by instagramViewModel.importUiState.collectAsStateWithLifecycle()

    val isImporting = isDetectingFile ||
        (awaitingImportPlatform == "whatsapp" && waImportState is ImportUiState.Loading) ||
        (awaitingImportPlatform == "instagram" && igImportState is ImportUiState.Loading)

    // Observe import completion / error when on Screen 4
    LaunchedEffect(awaitingImportPlatform, waImportState, igImportState, currentStep) {
        if (currentStep != OnboardingStep.SCREEN_4_WHATSAPP && currentStep != OnboardingStep.SCREEN_4_INSTAGRAM) {
            return@LaunchedEffect
        }
        val activeState = when (awaitingImportPlatform) {
            "whatsapp" -> waImportState
            "instagram" -> igImportState
            else -> null
        } ?: return@LaunchedEffect

        when (activeState) {
            is ImportUiState.Success -> {
                val imported = awaitingImportPlatform
                lastImportedPlatform = imported
                awaitingImportPlatform = null
                if (imported == "whatsapp") {
                    whatsappViewModel.dismissImportState()
                } else {
                    instagramViewModel.dismissImportState()
                }
                val message = if (imported == "whatsapp") {
                    strings.importedWhatsappSnackbar
                } else {
                    strings.importedInstagramSnackbar
                }
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
                delay(450)
                currentStep = OnboardingStep.SCREEN_5
            }
            is ImportUiState.Error -> {
                val errMsg = activeState.message
                if (awaitingImportPlatform == "whatsapp") {
                    whatsappViewModel.dismissImportState()
                } else {
                    instagramViewModel.dismissImportState()
                }
                awaitingImportPlatform = null
                snackbarHostState.showSnackbar(
                    message = errMsg,
                    duration = SnackbarDuration.Long
                )
            }
            else -> Unit
        }
    }

    // Back navigation within onboarding
    BackHandler(enabled = !showLanguageChanger && currentStep != OnboardingStep.SCREEN_1 && !isImporting) {
        currentStep = when (currentStep) {
            OnboardingStep.SCREEN_2 -> OnboardingStep.SCREEN_1
            OnboardingStep.SCREEN_3 -> OnboardingStep.SCREEN_2
            OnboardingStep.SCREEN_4_WHATSAPP, OnboardingStep.SCREEN_4_INSTAGRAM -> OnboardingStep.SCREEN_3
            OnboardingStep.SCREEN_5 -> if (lastImportedPlatform == "instagram") {
                OnboardingStep.SCREEN_4_INSTAGRAM
            } else {
                OnboardingStep.SCREEN_4_WHATSAPP
            }
            OnboardingStep.SCREEN_1 -> OnboardingStep.SCREEN_1
        }
    }

    // Document picker for WhatsApp (.txt / .zip) and Instagram (.zip)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                isDetectingFile = true
                val detectionResult = withContext(Dispatchers.IO) {
                    detectPickedArchive(context, uri)
                }
                isDetectingFile = false
                if (detectionResult == null) {
                    snackbarHostState.showSnackbar(strings.importUnrecognizedFileError)
                    return@launch
                }
                val (platform, fileName, tempFile) = detectionResult
                val tempUri = Uri.fromFile(tempFile)
                when (platform) {
                    DetectedArchivePlatform.WHATSAPP_TXT -> {
                        awaitingImportPlatform = "whatsapp"
                        whatsappViewModel.importChatUri(context.contentResolver, tempUri, fileName)
                    }
                    DetectedArchivePlatform.WHATSAPP_ZIP -> {
                        awaitingImportPlatform = "whatsapp"
                        whatsappViewModel.importZipUri(context.contentResolver, tempUri, fileName)
                    }
                    DetectedArchivePlatform.INSTAGRAM_ZIP -> {
                        awaitingImportPlatform = "instagram"
                        instagramViewModel.importZipUri(context.contentResolver, tempUri, fileName)
                    }
                    DetectedArchivePlatform.UNKNOWN -> {
                        tempFile.delete()
                        snackbarHostState.showSnackbar(strings.importUnrecognizedFileError)
                    }
                }
            }
        }
    )

    // Continuous axis rotations for all "Move" elements and "Outer-Yellow"
    val infiniteTransition = rememberInfiniteTransition(label = "OnboardingAxisRotations")
    val outerYellowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "OuterYellowAxis"
    )
    val verySunnyRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing)),
        label = "VerySunnyAxis"
    )
    val ghostishRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "GhostishAxis"
    )
    val fanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "FanAxis"
    )
    val arrowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing)),
        label = "ArrowAxis"
    )
    val starsRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing)),
        label = "StarsAxis"
    )

    // Screen 3 Fan -> Flower morph progress (cycles smoothly between Fan and Flower on Screen 3)
    val screen3MorphCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FanToFlowerMorph"
    )

    // Platform click transition progress (Arrow shower or Flower full-screen fill)
    val platformOverlayProgress = remember { Animatable(0f) }

    fun launchWhatsAppTransition() {
        if (transitionMode != PlatformTransitionMode.NONE) return
        scope.launch {
            transitionMode = PlatformTransitionMode.WHATSAPP_ARROWS
            platformOverlayProgress.snapTo(0f)
            platformOverlayProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            currentStep = OnboardingStep.SCREEN_4_WHATSAPP
            platformOverlayProgress.snapTo(0f)
            transitionMode = PlatformTransitionMode.NONE
        }
    }

    fun launchInstagramTransition() {
        if (transitionMode != PlatformTransitionMode.NONE) return
        scope.launch {
            transitionMode = PlatformTransitionMode.INSTAGRAM_FLOWER
            platformOverlayProgress.snapTo(0f)
            platformOverlayProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            currentStep = OnboardingStep.SCREEN_4_INSTAGRAM
            platformOverlayProgress.snapTo(0f)
            transitionMode = PlatformTransitionMode.NONE
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { _ ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(OnboardingBgTop, OnboardingBgBottom),
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            val screenW = maxWidth
            val screenH = maxHeight
            val minDim = minOf(screenW, screenH)

            // Bottom sky gradient overlay present on Screen 1 and Screen 5
            val skyTintAlpha by animateFloatAsState(
                targetValue = if (currentStep == OnboardingStep.SCREEN_1 || currentStep == OnboardingStep.SCREEN_5) 1f else 0f,
                animationSpec = tween(450),
                label = "SkyTintAlpha"
            )
            if (skyTintAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(skyTintAlpha)
                        .background(
                            Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1.0f to OnboardingSkyTintBottom
                            )
                        )
                )
            }

            // Screen 1 Giant Foreground Overlay (behind Screen 1 bottom CTA button so CTA remains crisp & clickable)
            val s1OverlayAlpha by animateFloatAsState(
                targetValue = if (currentStep == OnboardingStep.SCREEN_1) 1f else 0f,
                animationSpec = tween(420),
                label = "S1OverlayAlpha"
            )
            if (s1OverlayAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(s1OverlayAlpha)
                ) {
                    Screen1WasmHeroOverlay(
                        outerYellowRotation = outerYellowRotation,
                        starsRotation = starsRotation
                    )
                }
            }

            // =========================================================================
            // PERSISTENT SHARED ANIMATED ELEMENTS LAYER
            // Smoothly transitions Very Sunny, Ghost-ish, Arrow, and Fan/Flower across screens
            // =========================================================================
            val verySunnyTarget = when (currentStep) {
                OnboardingStep.SCREEN_1 -> ElementPlacement(49.4f / 412f, 106.4f / 917f, 118.4f / 412f)
                OnboardingStep.SCREEN_2 -> ElementPlacement(59.4f / 412f, 184.4f / 917f, 118.4f / 412f)
                OnboardingStep.SCREEN_3 -> ElementPlacement(202.4f / 412f, 186.4f / 917f, 118.4f / 412f)
                OnboardingStep.SCREEN_4_WHATSAPP -> ElementPlacement(206.4f / 412f, 174.4f / 917f, 118.4f / 412f)
                OnboardingStep.SCREEN_4_INSTAGRAM -> ElementPlacement(206.4f / 412f, 174.4f / 917f, 118.4f / 412f)
                OnboardingStep.SCREEN_5 -> ElementPlacement(52.5f / 412f, 464.5f / 917f, 89.1f / 412f)
            }

            val ghostishTarget = when (currentStep) {
                OnboardingStep.SCREEN_1 -> ElementPlacement(323.4f / 412f, 757.4f / 917f, 108f / 412f)
                OnboardingStep.SCREEN_2 -> ElementPlacement(354.4f / 412f, 511.4f / 917f, 108f / 412f)
                OnboardingStep.SCREEN_5 -> ElementPlacement(367.0f / 412f, 102.0f / 917f, 66.3f / 412f)
                else -> ElementPlacement(430f / 412f, 380f / 917f, 54f / 412f, visible = false)
            }

            val whatsappArrowTarget = when (currentStep) {
                OnboardingStep.SCREEN_1 -> ElementPlacement(222.0f / 412f, 602.8f / 917f, 116.4f / 412f)
                OnboardingStep.SCREEN_2 -> ElementPlacement(157.8f / 412f, 623.2f / 917f, 61.2f / 412f)
                OnboardingStep.SCREEN_3 -> ElementPlacement(106.2f / 412f, 515.9f / 917f, 138.5f / 412f)
                else -> ElementPlacement(106.2f / 412f, 515.9f / 917f, 60f / 412f, visible = false)
            }

            val instagramFanTarget = when (currentStep) {
                OnboardingStep.SCREEN_1 -> ElementPlacement(361.1f / 412f, 452.5f / 917f, 139.9f / 412f)
                OnboardingStep.SCREEN_2 -> ElementPlacement(245.5f / 412f, 623.9f / 917f, 62.2f / 412f)
                OnboardingStep.SCREEN_3 -> ElementPlacement(305.5f / 412f, 515.5f / 917f, 137.3f / 412f)
                else -> ElementPlacement(305.5f / 412f, 515.5f / 917f, 60f / 412f, visible = false)
            }

            // Shared Very Sunny / Flower badge (becomes WhatsApp neon or Instagram Flower on Screen 4!)
            SharedPositionedSlot(
                target = verySunnyTarget,
                screenW = screenW,
                screenH = screenH,
                minDim = minDim,
                label = "VerySunnyShared"
            ) {
                val badgeColor by animateColorAsState(
                    targetValue = when (currentStep) {
                        OnboardingStep.SCREEN_4_WHATSAPP -> OnboardingWhatsAppNeon
                        else -> Color.White
                    },
                    animationSpec = tween(360),
                    label = "VerySunnyColor"
                )
                when (currentStep) {
                    OnboardingStep.SCREEN_4_WHATSAPP -> {
                        ExpressiveGroupedBadge(
                            shapePathData = OnboardingSvgPaths.verySunnyPath,
                            shapeSrcWidth = OnboardingSvgPaths.verySunnyWidth,
                            shapeSrcHeight = OnboardingSvgPaths.verySunnyHeight,
                            shapeColor = badgeColor,
                            shapeRotation = verySunnyRotation,
                            iconPaths = listOf(OnboardingSvgPaths.whatsappIconPath),
                            iconColors = listOf(Color.White),
                            iconSrcWidth = OnboardingSvgPaths.whatsappIconWidth,
                            iconSrcHeight = OnboardingSvgPaths.whatsappIconHeight,
                            iconScaleFraction = 0.48f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    OnboardingStep.SCREEN_4_INSTAGRAM -> {
                        ExpressiveGroupedBadge(
                            shapePathData = OnboardingSvgPaths.verySunnyPath,
                            shapeSrcWidth = OnboardingSvgPaths.verySunnyWidth,
                            shapeSrcHeight = OnboardingSvgPaths.verySunnyHeight,
                            shapeColor = null,
                            shapeBrush = InstagramBadgeBrush,
                            shapeRotation = verySunnyRotation,
                            iconPaths = OnboardingSvgPaths.instagramIconPaths,
                            iconColors = listOf(Color.White, Color.White, Color.White),
                            iconSrcWidth = OnboardingSvgPaths.instagramIconWidth,
                            iconSrcHeight = OnboardingSvgPaths.instagramIconHeight,
                            iconScaleFraction = 0.46f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        ExpressiveGroupedBadge(
                            shapePathData = OnboardingSvgPaths.verySunnyPath,
                            shapeSrcWidth = OnboardingSvgPaths.verySunnyWidth,
                            shapeSrcHeight = OnboardingSvgPaths.verySunnyHeight,
                            shapeColor = badgeColor,
                            shapeRotation = verySunnyRotation,
                            iconPaths = OnboardingSvgPaths.chatsIconPaths,
                            iconColors = listOf(Color.Black, Color.Black),
                            iconSrcWidth = OnboardingSvgPaths.chatsIconWidth,
                            iconSrcHeight = OnboardingSvgPaths.chatsIconHeight,
                            iconScaleFraction = 0.44f,
                            iconStrokeWidths = listOf(3.94f, 3.94f),
                            iconFillFlags = listOf(false, true),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Shared Ghost-ish (File) badge
            SharedPositionedSlot(
                target = ghostishTarget,
                screenW = screenW,
                screenH = screenH,
                minDim = minDim,
                label = "GhostishShared"
            ) {
                ExpressiveGroupedBadge(
                    shapePathData = OnboardingSvgPaths.ghostishPath,
                    shapeSrcWidth = OnboardingSvgPaths.ghostishWidth,
                    shapeSrcHeight = OnboardingSvgPaths.ghostishHeight,
                    shapeColor = OnboardingGhostRed,
                    shapeRotation = ghostishRotation,
                    iconPaths = OnboardingSvgPaths.filePlusIconPaths,
                    iconColors = listOf(Color.Black, Color.Black),
                    iconSrcWidth = OnboardingSvgPaths.filePlusIconWidth,
                    iconSrcHeight = OnboardingSvgPaths.filePlusIconHeight,
                    iconScaleFraction = 0.48f,
                    iconStrokeWidths = listOf(3.94f, 0f),
                    iconFillFlags = listOf(false, true),
                    useEvenOddFill = true,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Shared WhatsApp Arrow badge (clickable on Screen 3!)
            SharedPositionedSlot(
                target = whatsappArrowTarget,
                screenW = screenW,
                screenH = screenH,
                minDim = minDim,
                label = "WhatsappArrowShared"
            ) {
                ExpressiveGroupedBadge(
                    shapePathData = OnboardingSvgPaths.arrowShapePath,
                    shapeSrcWidth = OnboardingSvgPaths.arrowShapeWidth,
                    shapeSrcHeight = OnboardingSvgPaths.arrowShapeHeight,
                    shapeColor = OnboardingWhatsAppArrowNeon,
                    shapeRotation = arrowRotation,
                    iconPaths = listOf(OnboardingSvgPaths.whatsappIconPath),
                    iconColors = listOf(Color.Black),
                    iconSrcWidth = OnboardingSvgPaths.whatsappIconWidth,
                    iconSrcHeight = OnboardingSvgPaths.whatsappIconHeight,
                    iconScaleFraction = 0.45f,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (currentStep == OnboardingStep.SCREEN_3) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { launchWhatsAppTransition() }
                                )
                            } else {
                                Modifier
                            }
                        )
                )
            }

            // Shared Instagram Fan badge -> morphs into Flower on Screen 3 (clickable on Screen 3!)
            SharedPositionedSlot(
                target = instagramFanTarget,
                screenW = screenW,
                screenH = screenH,
                minDim = minDim,
                label = "InstagramFanShared"
            ) {
                val clickMod = if (currentStep == OnboardingStep.SCREEN_3) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { launchInstagramTransition() }
                    )
                } else {
                    Modifier
                }

                if (currentStep == OnboardingStep.SCREEN_3) {
                    FanToFlowerMorphBadge(
                        morphProgress = screen3MorphCycle,
                        rotationDegrees = fanRotation,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(clickMod)
                    )
                } else {
                    ExpressiveGroupedBadge(
                        shapePathData = OnboardingSvgPaths.fanPath,
                        shapeSrcWidth = OnboardingSvgPaths.fanWidth,
                        shapeSrcHeight = OnboardingSvgPaths.fanHeight,
                        shapeColor = OnboardingInstagramAmber,
                        shapeRotation = fanRotation,
                        iconPaths = OnboardingSvgPaths.instagramIconPaths,
                        iconColors = listOf(Color(0xFF0F0F0F), Color(0xFF0F0F0F), Color(0xFF0F0F0F)),
                        iconSrcWidth = OnboardingSvgPaths.instagramIconWidth,
                        iconSrcHeight = OnboardingSvgPaths.instagramIconHeight,
                        iconScaleFraction = 0.44f,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(clickMod)
                    )
                }
            }

            // =========================================================================
            // PER-SCREEN FOREGROUND CONTENT & DIRECTIONAL TRANSITIONS
            // =========================================================================
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    when {
                        // WhatsApp clicked: down-to-up transition
                        targetState == OnboardingStep.SCREEN_4_WHATSAPP -> {
                            (slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(320)))
                                .togetherWith(slideOutVertically(tween(360)) { -it / 3 } + fadeOut(tween(240)))
                        }
                        // Instagram clicked: up-to-down transition
                        targetState == OnboardingStep.SCREEN_4_INSTAGRAM -> {
                            (slideInVertically(tween(420, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(320)))
                                .togetherWith(slideOutVertically(tween(360)) { it / 3 } + fadeOut(tween(240)))
                        }
                        // Back from Screen 4 to Screen 3
                        initialState == OnboardingStep.SCREEN_4_WHATSAPP && targetState == OnboardingStep.SCREEN_3 -> {
                            (slideInVertically(tween(380)) { -it / 2 } + fadeIn(tween(300)))
                                .togetherWith(slideOutVertically(tween(380)) { it } + fadeOut(tween(260)))
                        }
                        initialState == OnboardingStep.SCREEN_4_INSTAGRAM && targetState == OnboardingStep.SCREEN_3 -> {
                            (slideInVertically(tween(380)) { it / 2 } + fadeIn(tween(300)))
                                .togetherWith(slideOutVertically(tween(380)) { -it } + fadeOut(tween(260)))
                        }
                        // Forward progression
                        targetState.ordinal > initialState.ordinal -> {
                            (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it / 4 } + fadeIn(tween(340)))
                                .togetherWith(slideOutHorizontally(tween(320)) { -it / 4 } + fadeOut(tween(240)))
                        }
                        else -> {
                            (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { -it / 4 } + fadeIn(tween(340)))
                                .togetherWith(slideOutHorizontally(tween(320)) { it / 4 } + fadeOut(tween(240)))
                        }
                    }
                },
                label = "OnboardingScreenTransition",
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (step) {
                    OnboardingStep.SCREEN_1 -> OnboardingScreen1Content(
                        ctaText = strings.getStarted,
                        onGetStarted = { currentStep = OnboardingStep.SCREEN_2 }
                    )
                    OnboardingStep.SCREEN_2 -> OnboardingScreen2Content(
                        helloText = strings.hello,
                        nativelySupportsText = strings.nativelySupports,
                        platformsText = strings.whatsappAndInstagramAsterisk,
                        copyrightText = strings.metaCopyright,
                        nextText = strings.next,
                        onNext = { currentStep = OnboardingStep.SCREEN_3 }
                    )
                    OnboardingStep.SCREEN_3 -> OnboardingScreen3Content(
                        headingText = strings.howDoYouWantToStart,
                        whatsappLabel = strings.whatsapp,
                        instagramLabel = strings.instagram,
                        copyrightText = strings.metaCopyright,
                        onSelectWhatsApp = { launchWhatsAppTransition() },
                        onSelectInstagram = { launchInstagramTransition() }
                    )
                    OnboardingStep.SCREEN_4_WHATSAPP -> OnboardingScreen4Content(
                        title = strings.whatsapp,
                        accentColor = OnboardingWhatsAppAccent,
                        instructionsHeading = strings.instructionsHeading,
                        instructionsSteps = strings.whatsappInstructionsSteps,
                        importHint = strings.importRequirementHint,
                        importButtonText = strings.importFile,
                        isImporting = isImporting,
                        importingStatusText = when {
                            isDetectingFile -> strings.importingWait
                            waImportState is ImportUiState.Loading -> (waImportState as ImportUiState.Loading).step
                            else -> strings.importingWait
                        },
                        onBack = { if (!isImporting) currentStep = OnboardingStep.SCREEN_3 },
                        onImportClick = {
                            if (!isImporting) {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "text/plain",
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                        }
                    )
                    OnboardingStep.SCREEN_4_INSTAGRAM -> OnboardingScreen4Content(
                        title = strings.instagram,
                        accentColor = OnboardingInstagramCyan,
                        instructionsHeading = strings.instructionsHeading,
                        instructionsSteps = strings.instagramInstructionsSteps,
                        importHint = strings.importRequirementHint,
                        importButtonText = strings.importFile,
                        isImporting = isImporting,
                        importingStatusText = when {
                            isDetectingFile -> strings.importingWait
                            igImportState is ImportUiState.Loading -> (igImportState as ImportUiState.Loading).step
                            else -> strings.importingWait
                        },
                        onBack = { if (!isImporting) currentStep = OnboardingStep.SCREEN_3 },
                        onImportClick = {
                            if (!isImporting) {
                                filePickerLauncher.launch(
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
                    )
                    OnboardingStep.SCREEN_5 -> OnboardingScreen5Content(
                        outerYellowRotation = outerYellowRotation,
                        everythingIsSetText = strings.everythingIsSet,
                        letsDiveInText = strings.letsDiveIn,
                        onDiveIn = { onFinishOnboarding(lastImportedPlatform) }
                    )
                }
            }

            // =========================================================================
            // TOP SECTION: Language Changer Pill (Screens 1 - 4)
            // =========================================================================
            if (currentStep != OnboardingStep.SCREEN_5) {
                val activeLang = languageByCode(selectedLanguageCode)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        onClick = { if (!isImporting) showLanguageChanger = true },
                        shape = CircleShape,
                        color = OnboardingLangPillBg,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(text = activeLang.flagEmoji, fontSize = 13.sp)
                            Text(
                                text = activeLang.code,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // EXPRESSIVE PLATFORM CLICK TRANSITION OVERLAYS (WhatsApp Arrows / IG Flower Fill)
            // =========================================================================
            when (transitionMode) {
                PlatformTransitionMode.WHATSAPP_ARROWS -> {
                    ExpressiveArrowShowerOverlay(progress = platformOverlayProgress.value)
                }
                PlatformTransitionMode.INSTAGRAM_FLOWER -> {
                    ExpressiveFlowerFillOverlay(
                        progress = platformOverlayProgress.value,
                        originFraction = Offset(305.5f / 412f, 515.5f / 917f)
                    )
                }
                PlatformTransitionMode.NONE -> Unit
            }

            // =========================================================================
            // LANGUAGE CHANGER MODAL OVERLAY
            // =========================================================================
            AnimatedVisibility(
                visible = showLanguageChanger,
                enter = slideInVertically(tween(340, easing = FastOutSlowInEasing)) { -it / 4 } + fadeIn(tween(260)),
                exit = slideOutVertically(tween(280)) { -it / 4 } + fadeOut(tween(220))
            ) {
                LanguageChangerScreen(
                    selectedLanguageCode = selectedLanguageCode,
                    onSelectLanguage = { lang ->
                        scope.launch {
                            prefs.saveAppLanguage(lang.code)
                        }
                        showLanguageChanger = false
                    },
                    onBack = { showLanguageChanger = false }
                )
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.SharedPositionedSlot(
    target: ElementPlacement,
    screenW: Dp,
    screenH: Dp,
    minDim: Dp,
    label: String,
    content: @Composable BoxScope.() -> Unit
) {
    val springSpec = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow
    )
    val xFrac by animateFloatAsState(target.xFraction, springSpec, label = "${label}_x")
    val yFrac by animateFloatAsState(target.yFraction, springSpec, label = "${label}_y")
    val sizeFrac by animateFloatAsState(target.sizeFraction, springSpec, label = "${label}_s")
    val alpha by animateFloatAsState(if (target.visible) 1f else 0f, tween(280), label = "${label}_a")

    if (alpha <= 0.01f) return
    val sizeDp = minDim * sizeFrac
    val offsetX = screenW * xFrac - sizeDp / 2f
    val offsetY = screenH * yFrac - sizeDp / 2f

    Box(
        modifier = Modifier
            .absoluteOffset(x = offsetX, y = offsetY)
            .size(sizeDp)
            .alpha(alpha),
        content = content
    )
}

@Composable
private fun OnboardingPrimaryButton(
    text: String,
    leadingImportIcon: Boolean = false,
    trailingArrow: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowPath = rememberSvgPath(OnboardingSvgPaths.buttonArrowPath)
    val unionPaths = remember {
        OnboardingSvgPaths.importUnionPaths.map { PathParser().parsePathString(it).toPath() }
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(41.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OnboardingButtonBlue,
            contentColor = Color.White,
            disabledContainerColor = OnboardingButtonBlue.copy(alpha = 0.55f),
            disabledContentColor = Color.White.copy(alpha = 0.65f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingImportIcon) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val scale = min(
                        size.width / OnboardingSvgPaths.importUnionWidth,
                        size.height / OnboardingSvgPaths.importUnionHeight
                    )
                    val dx = (size.width - OnboardingSvgPaths.importUnionWidth * scale) / 2f
                    val dy = (size.height - OnboardingSvgPaths.importUnionHeight * scale) / 2f
                    withTransform({
                        translate(dx, dy)
                        scale(scale, scale, pivot = Offset.Zero)
                    }) {
                        unionPaths.forEach { drawPath(it, color = Color.White) }
                    }
                }
                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp
                )
            )

            if (trailingArrow) {
                Spacer(Modifier.width(14.dp))
                Canvas(modifier = Modifier.size(width = 26.dp, height = 20.dp)) {
                    drawNormalizedPath(
                        path = arrowPath,
                        srcWidth = OnboardingSvgPaths.buttonArrowWidth,
                        srcHeight = OnboardingSvgPaths.buttonArrowHeight,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen1Content(
    ctaText: String,
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        OnboardingPrimaryButton(
            text = ctaText,
            trailingArrow = true,
            onClick = onGetStarted
        )
    }
}

@Composable
private fun OnboardingScreen2Content(
    helloText: String,
    nativelySupportsText: String,
    platformsText: String,
    copyrightText: String,
    nextText: String,
    onNext: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        val h = maxHeight

        // Middle Section: Apple-style animated multi-language "Hello"
        val helloGreetings = remember(helloText) {
            listOf(
                helloText,
                "Hello",
                "नमस्ते",
                "こんにちは",
                "Hola",
                "Привет",
                "হ্যালো",
                "Olá",
                "Halo",
                "ہیلو",
                "Hallo",
                "ਸਤ ਸ੍ਰੀ ਅਕਾਲ"
            ).distinct()
        }
        var greetingIndex by remember { mutableIntStateOf(0) }
        LaunchedEffect(helloGreetings) {
            greetingIndex = 0
            while (true) {
                delay(1850L)
                greetingIndex = (greetingIndex + 1) % helloGreetings.size
            }
        }
        val activeGreeting = helloGreetings[greetingIndex]
        val greetingFontSize = if (activeGreeting.length > 9) 46.sp else 60.sp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = h * 0.34f)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = activeGreeting,
                transitionSpec = {
                    (slideInVertically(tween(520, easing = FastOutSlowInEasing)) { it / 2 } +
                        fadeIn(tween(480)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(520)))
                        .togetherWith(
                            slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { -it / 2 } +
                                fadeOut(tween(340)) +
                                scaleOut(targetScale = 1.05f, animationSpec = tween(420))
                        )
                },
                label = "AppleStyleHelloTransition"
            ) { greeting ->
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = greetingFontSize,
                        letterSpacing = (-1.0).sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        // Middle - Bottom Section: "Natively Supports" & "Whatsapp & Instagram*" below shared social icons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = h * 0.72f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = nativelySupportsText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 19.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = platformsText,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Bottom Section: Meta copyright + Next CTA
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = copyrightText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            OnboardingPrimaryButton(
                text = nextText,
                trailingArrow = true,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun OnboardingScreen3Content(
    headingText: String,
    whatsappLabel: String,
    instagramLabel: String,
    copyrightText: String,
    onSelectWhatsApp: () -> Unit,
    onSelectInstagram: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        val h = maxHeight

        // top - Middle Section heading below shared Very Sunny badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = h * 0.305f)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = headingText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 33.sp,
                    lineHeight = 40.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Middle section: Interactive labels directly underneath the shared Arrow & Fan/Flower icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = h * 0.655f)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = whatsappLabel,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onSelectWhatsApp)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
            Text(
                text = instagramLabel,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onSelectInstagram)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Middle Down Section: Meta copyright notice
        Text(
            text = copyrightText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color.White.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 40.dp)
        )
    }
}

@Composable
private fun OnboardingScreen4Content(
    title: String,
    accentColor: Color,
    instructionsHeading: String,
    instructionsSteps: List<String>,
    importHint: String,
    importButtonText: String,
    isImporting: Boolean,
    importingStatusText: String,
    onBack: () -> Unit,
    onImportClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val w = maxWidth
        val h = maxHeight

        // Back button at top-left matching Top Section height
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 12.dp)
                .align(Alignment.TopStart)
        ) {
            Surface(
                onClick = onBack,
                enabled = !isImporting,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.16f),
                contentColor = Color.White,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Space reserved for shared Very Sunny / Flower badge in Top Middle Section
            Spacer(modifier = Modifier.height(h * 0.25f))

            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 48.sp,
                    letterSpacing = (-1.0).sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Middle Down Section: Dark Instructions card that morphs into M3 Expressive LoadingIndicator circle!
            val targetCardWidth = if (isImporting) 172.dp else (w - 56.dp).coerceAtLeast(240.dp)
            val targetCardHeight = if (isImporting) 172.dp else (h * 0.40f).coerceIn(220.dp, 387.dp)
            val targetCorner = if (isImporting) 86.dp else 16.dp

            val cardWidth by animateDpAsState(
                targetValue = targetCardWidth,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "CardMorphWidth"
            )
            val cardHeight by animateDpAsState(
                targetValue = targetCardHeight,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "CardMorphHeight"
            )
            val cardCorner by animateDpAsState(
                targetValue = targetCorner,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "CardMorphCorner"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(cardCorner),
                    color = OnboardingDarkCard,
                    modifier = Modifier.size(width = cardWidth, height = cardHeight)
                ) {
                    AnimatedContent(
                        targetState = isImporting,
                        transitionSpec = {
                            fadeIn(tween(260)) togetherWith fadeOut(tween(180))
                        },
                        label = "InstructionsToLoaderMorph"
                    ) { loading ->
                        if (loading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                MorphingExpressiveLoadingIndicator(
                                    color = accentColor,
                                    modifier = Modifier.size(86.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = importingStatusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = instructionsHeading,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = accentColor
                                )
                                instructionsSteps.forEach { step ->
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 22.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = importHint,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor.copy(alpha = 0.80f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Section: Import File CTA
            OnboardingPrimaryButton(
                text = importButtonText,
                leadingImportIcon = true,
                enabled = !isImporting,
                onClick = onImportClick,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun OnboardingScreen5Content(
    outerYellowRotation: Float,
    everythingIsSetText: String,
    letsDiveInText: String,
    onDiveIn: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        val minDim = minOf(maxWidth, maxHeight)
        val h = maxHeight
        val logoCardSize = minDim * (253f / 412f)

        // Top Middle Section: WASM - Logo card with rotating Outer-Yellow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = h * 0.145f),
            contentAlignment = Alignment.TopCenter
        ) {
            Screen5WasmLogoCard(
                outerYellowRotation = outerYellowRotation,
                cardSize = logoCardSize
            )
        }

        // Down Section: "Everything is set" + "Let's Dive In ->"
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = everythingIsSetText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 48.sp,
                    lineHeight = 54.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(38.dp))
            OnboardingPrimaryButton(
                text = letsDiveInText,
                trailingArrow = true,
                onClick = onDiveIn
            )
        }
    }
}

private fun detectPickedArchive(
    context: Context,
    uri: Uri
): Triple<DetectedArchivePlatform, String?, File>? {
    val fileName = queryDisplayName(context, uri)
    val tempDir = File(context.cacheDir, "onboarding_imports").apply {
        mkdirs()
        listFiles()?.forEach { it.delete() }
    }
    val safeExt = if (fileName?.endsWith(".txt", ignoreCase = true) == true) ".txt" else ".zip"
    val tempFile = File(tempDir, "import_${System.currentTimeMillis()}$safeExt")

    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null

        val platform = ZipArchiveDetector.detectFromFile(tempFile, fileName)
        Triple(platform, fileName, tempFile)
    } catch (_: Exception) {
        tempFile.delete()
        null
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1) name = cursor.getString(idx)
        }
    }
    return name
}
