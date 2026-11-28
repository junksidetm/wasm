package com.mrdartsidetm.wasm

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrdartsidetm.wasm.data.ChatDatabase
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.ChatScreen
import com.mrdartsidetm.wasm.ui.ChatViewModel
import com.mrdartsidetm.wasm.ui.HomeScreen
import com.mrdartsidetm.wasm.ui.ImportScreen
import com.mrdartsidetm.wasm.ui.SettingsScreen
import com.mrdartsidetm.wasm.ui.StorageDataScreen
import com.mrdartsidetm.wasm.ui.StorageScreen
import com.mrdartsidetm.wasm.ui.WallpaperScreen
import com.mrdartsidetm.wasm.ui.instagram.InstagramScreen
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.localization.languageByCode
import com.mrdartsidetm.wasm.ui.localization.wasmStringsFor
import com.mrdartsidetm.wasm.ui.motion.WasmConfirmationCapsule
import com.mrdartsidetm.wasm.ui.motion.WasmConfirmationPresenter
import com.mrdartsidetm.wasm.ui.motion.WasmDock
import com.mrdartsidetm.wasm.ui.motion.WasmMotionTokens
import com.mrdartsidetm.wasm.ui.motion.rememberAnimatorsEnabled
import com.mrdartsidetm.wasm.ui.onboarding.OnboardingBgBottom
import com.mrdartsidetm.wasm.ui.onboarding.OnboardingFlow
import com.mrdartsidetm.wasm.ui.theme.AppFontOption
import com.mrdartsidetm.wasm.ui.theme.ChatWallpaperConfig
import com.mrdartsidetm.wasm.ui.theme.ChatWallpaperPreset
import com.mrdartsidetm.wasm.ui.theme.ColorVariantOption
import com.mrdartsidetm.wasm.ui.theme.DynamicVariantOption
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexAxisConfig
import com.mrdartsidetm.wasm.ui.theme.LocalChatWallpaper
import com.mrdartsidetm.wasm.ui.theme.ThemeModeOption
import com.mrdartsidetm.wasm.ui.theme.WasmTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val db = remember { ChatDatabase.getDatabase(applicationContext) }
            val prefs = remember { UserPreferencesRepository(applicationContext) }
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeModeOption.SYSTEM.id)
            val dynamicColorEnabled by prefs.dynamicColorEnabled.collectAsStateWithLifecycle(initialValue = true)
            val colorVariant by prefs.colorVariant.collectAsStateWithLifecycle(initialValue = ColorVariantOption.DEFAULT.id)
            val dynamicVariant by prefs.dynamicVariant.collectAsStateWithLifecycle(initialValue = DynamicVariantOption.TONAL_SPOT.id)
            val chatWallpaper by prefs.chatWallpaper.collectAsStateWithLifecycle(initialValue = ChatWallpaperPreset.DEFAULT.id)
            val chatWallpaperImagePath by prefs.chatWallpaperImagePath.collectAsStateWithLifecycle(initialValue = null)
            val fontSelection by prefs.fontSelection.collectAsStateWithLifecycle(initialValue = AppFontOption.GOOGLE_SANS_FLEX.id)
            val emojiFont by prefs.emojiFont.collectAsStateWithLifecycle(initialValue = com.mrdartsidetm.wasm.ui.theme.EmojiPackOption.DEFAULT.id)
            val emojiRevision by prefs.emojiRevision.collectAsStateWithLifecycle(initialValue = 0)
            val gsfGrade by prefs.gsfGrade.collectAsStateWithLifecycle(initialValue = 0f)
            val gsfWeight by prefs.gsfWeight.collectAsStateWithLifecycle(initialValue = 400f)
            val gsfWidth by prefs.gsfWidth.collectAsStateWithLifecycle(initialValue = 100f)
            val gsfRoundness by prefs.gsfRoundness.collectAsStateWithLifecycle(initialValue = 100f)
            val gsfOpticalSize by prefs.gsfOpticalSize.collectAsStateWithLifecycle(initialValue = 24f)
            val gsfSlant by prefs.gsfSlant.collectAsStateWithLifecycle(initialValue = 0f)

            val axisConfig = remember(gsfGrade, gsfWeight, gsfWidth, gsfRoundness, gsfOpticalSize, gsfSlant) {
                GoogleSansFlexAxisConfig(
                    grade = gsfGrade,
                    weight = gsfWeight,
                    width = gsfWidth,
                    roundness = gsfRoundness,
                    opticalSize = gsfOpticalSize,
                    slant = gsfSlant
                )
            }

            WasmTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColorEnabled,
                colorVariant = colorVariant,
                dynamicVariant = dynamicVariant,
                fontSelection = fontSelection,
                axisConfig = axisConfig,
                emojiFont = emojiFont,
                emojiRevision = emojiRevision
            ) {
                val coroutineScope = rememberCoroutineScope()
                val whatsappMediaDir = remember { File(applicationContext.filesDir, "media") }
                val instagramBaseDir = remember { File(applicationContext.filesDir, "instagram_data") }

                val appLanguage by prefs.appLanguage.collectAsStateWithLifecycle(initialValue = "en")
                val onboardingCompleted by prefs.onboardingCompleted.collectAsStateWithLifecycle(initialValue = null)
                var forceShowOnboarding by rememberSaveable { mutableStateOf(false) }

                val strings = remember(appLanguage) { wasmStringsFor(appLanguage) }
                val layoutDirection = remember(appLanguage) {
                    if (languageByCode(appLanguage).isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                }

                // WhatsApp ViewModel
                val whatsappViewModel: ChatViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChatViewModel(db.chatDao(), prefs, whatsappMediaDir) as T
                        }
                    }
                )

                // Instagram ViewModel
                val instagramViewModel: InstagramViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return InstagramViewModel(db.instagramDao(), instagramBaseDir) as T
                        }
                    }
                )

                // Selected Tab: 0 = Home, 1 = Storage, 2 = Settings
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                // Active platform thread opened from Home (null = show main tab)
                var activeMessagesPlatform by rememberSaveable { mutableStateOf<String?>(null) }
                // Preferred platform tab to show on Home after importing
                var homePreferredPlatform by rememberSaveable { mutableStateOf<String?>(null) }
                // Full-screen Import Screen modal launched by Home FAB
                var showImportScreen by rememberSaveable { mutableStateOf(false) }
                // Tracks whether Settings is currently showing a sub-screen (General, Language Changer, About)
                var isSettingsSubScreen by rememberSaveable { mutableStateOf(false) }
                // Direct modal overlays for Wallpaper HUD (opened from chat 3-dots) and Storage & Data (opened from Storage tab)
                var showWallpaperModal by rememberSaveable { mutableStateOf(false) }
                var showStorageDataModal by rememberSaveable { mutableStateOf(false) }

                var staggerEntranceToken by rememberSaveable { mutableIntStateOf(0) }
                val confirmations = remember(coroutineScope) { WasmConfirmationPresenter(coroutineScope) }
                val animatorsEnabled = rememberAnimatorsEnabled()
                val showOnboarding = onboardingCompleted == false || forceShowOnboarding

                CompositionLocalProvider(
                    LocalWasmStrings provides strings,
                    LocalLayoutDirection provides layoutDirection,
                    LocalChatWallpaper provides ChatWallpaperConfig(chatWallpaper, chatWallpaperImagePath)
                ) {
                    if (onboardingCompleted == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(OnboardingBgBottom)
                        )
                    } else {
                        val onboardingTransition = updateTransition(
                            targetState = showOnboarding,
                            label = "wasmOnboardingTransition"
                        )
                        onboardingTransition.AnimatedContent(
                            transitionSpec = {
                                if (!targetState && animatorsEnabled) {
                                    (slideInHorizontally(
                                        animationSpec = spring(stiffness = 380f, dampingRatio = 0.84f),
                                        initialOffsetX = { width -> width }
                                    ) + fadeIn(tween(220))) togetherWith
                                        (slideOutHorizontally(
                                            animationSpec = spring(stiffness = 380f, dampingRatio = 0.84f),
                                            targetOffsetX = { width -> -width }
                                        ) + fadeOut(tween(200)))
                                } else {
                                    fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { isOnboardingActive ->
                            if (isOnboardingActive) {
                                OnboardingFlow(
                                    whatsappViewModel = whatsappViewModel,
                                    instagramViewModel = instagramViewModel,
                                    prefs = prefs,
                                    selectedLanguageCode = appLanguage,
                                    onFinishOnboarding = { importedPlatform ->
                                        coroutineScope.launch {
                                            prefs.setOnboardingCompleted(true)
                                        }
                                        staggerEntranceToken++
                                        forceShowOnboarding = false
                                        if (importedPlatform != null) {
                                            homePreferredPlatform = importedPlatform
                                        }
                                        activeMessagesPlatform = null
                                        showImportScreen = false
                                        isSettingsSubScreen = false
                                        showWallpaperModal = false
                                        showStorageDataModal = false
                                        selectedTab = 0
                                    }
                                )
                            } else {
                                WasmMainWorkspace(
                                    prefs = prefs,
                                    whatsappViewModel = whatsappViewModel,
                                    instagramViewModel = instagramViewModel,
                                    appLanguage = appLanguage,
                                    selectedTab = selectedTab,
                                    onSelectTab = { selectedTab = it },
                                    activeMessagesPlatform = activeMessagesPlatform,
                                    onSetActiveMessagesPlatform = { activeMessagesPlatform = it },
                                    homePreferredPlatform = homePreferredPlatform,
                                    onSetHomePreferredPlatform = { homePreferredPlatform = it },
                                    showImportScreen = showImportScreen,
                                    onSetShowImportScreen = { showImportScreen = it },
                                    isSettingsSubScreen = isSettingsSubScreen,
                                    onSetSettingsSubScreen = { isSettingsSubScreen = it },
                                    showWallpaperModal = showWallpaperModal,
                                    onSetShowWallpaperModal = { showWallpaperModal = it },
                                    showStorageDataModal = showStorageDataModal,
                                    onSetShowStorageDataModal = { showStorageDataModal = it },
                                    staggerEntranceToken = staggerEntranceToken,
                                    onStaggerConsumed = { staggerEntranceToken = 0 },
                                    confirmations = confirmations,
                                    onReplayOnboarding = { forceShowOnboarding = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun WasmMainWorkspace(
        prefs: UserPreferencesRepository,
        whatsappViewModel: ChatViewModel,
        instagramViewModel: InstagramViewModel,
        appLanguage: String,
        selectedTab: Int,
        onSelectTab: (Int) -> Unit,
        activeMessagesPlatform: String?,
        onSetActiveMessagesPlatform: (String?) -> Unit,
        homePreferredPlatform: String?,
        onSetHomePreferredPlatform: (String?) -> Unit,
        showImportScreen: Boolean,
        onSetShowImportScreen: (Boolean) -> Unit,
        isSettingsSubScreen: Boolean,
        onSetSettingsSubScreen: (Boolean) -> Unit,
        showWallpaperModal: Boolean,
        onSetShowWallpaperModal: (Boolean) -> Unit,
        showStorageDataModal: Boolean,
        onSetShowStorageDataModal: (Boolean) -> Unit,
        staggerEntranceToken: Int,
        onStaggerConsumed: () -> Unit,
        confirmations: WasmConfirmationPresenter,
        onReplayOnboarding: () -> Unit
    ) {
        val strings = LocalWasmStrings.current
        val scope = rememberCoroutineScope()
        val animatorsEnabled = rememberAnimatorsEnabled()
        val ld = LocalLayoutDirection.current

        val waSelectedConversationId by whatsappViewModel.selectedConversationId.collectAsStateWithLifecycle()
        val igSelectedConversationId by instagramViewModel.selectedConversationId.collectAsStateWithLifecycle()
        val igIsViewingDetails by instagramViewModel.isViewingConversationDetails.collectAsStateWithLifecycle()
        val igExpandedMediaList by instagramViewModel.activeExpandedMediaList.collectAsStateWithLifecycle()
        var pendingGoToChat by remember { mutableStateOf<Pair<String, String>?>(null) }

        // Track full-screen child destinations separately from the 3-tab pager for smooth layer depth + PredictiveBack
        val hasTopOverlay = showWallpaperModal || showStorageDataModal || showImportScreen || activeMessagesPlatform != null
        val overlayProgress = remember { Animatable(if (hasTopOverlay) 1f else 0f) }
        var renderedOverlayKey by remember { mutableStateOf<String?>(null) }

        val currentOverlayKey = when {
            showWallpaperModal -> "wallpaper"
            showStorageDataModal -> "storage_data"
            showImportScreen -> "import"
            activeMessagesPlatform == "whatsapp" -> "whatsapp"
            activeMessagesPlatform == "instagram" -> "instagram"
            else -> null
        }

        LaunchedEffect(currentOverlayKey, animatorsEnabled) {
            if (currentOverlayKey != null) {
                val isFirstMount = renderedOverlayKey == null
                renderedOverlayKey = currentOverlayKey
                if (isFirstMount) {
                    if (animatorsEnabled) {
                        overlayProgress.snapTo(0f)
                        overlayProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                        )
                    } else {
                        overlayProgress.snapTo(1f)
                    }
                } else {
                    overlayProgress.snapTo(1f)
                }
            } else if (renderedOverlayKey != null) {
                if (animatorsEnabled) {
                    overlayProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
                } else {
                    overlayProgress.snapTo(0f)
                }
                renderedOverlayKey = null
            }
        }

        val closeTopOverlayAction: () -> Unit = {
            when {
                showWallpaperModal -> onSetShowWallpaperModal(false)
                showStorageDataModal -> onSetShowStorageDataModal(false)
                showImportScreen -> onSetShowImportScreen(false)
                activeMessagesPlatform == "whatsapp" -> {
                    if (waSelectedConversationId != null) {
                        whatsappViewModel.closeConversation()
                    }
                    onSetActiveMessagesPlatform(null)
                    onSelectTab(0)
                }
                activeMessagesPlatform == "instagram" -> {
                    when {
                        igExpandedMediaList != null -> instagramViewModel.closeExpandedMedia()
                        igIsViewingDetails -> instagramViewModel.closeConversationDetails()
                        igSelectedConversationId != null -> {
                            instagramViewModel.closeConversation()
                            onSetActiveMessagesPlatform(null)
                            onSelectTab(0)
                        }
                        else -> {
                            onSetActiveMessagesPlatform(null)
                            onSelectTab(0)
                        }
                    }
                }
            }
        }

        // Interactive PredictiveBack when overlay is directly dismissible to underlying screen
        val canScrubOverlayBack = hasTopOverlay && !(activeMessagesPlatform == "instagram" && (igExpandedMediaList != null || igIsViewingDetails))
        PredictiveBackHandler(enabled = canScrubOverlayBack) { backEvents ->
            try {
                backEvents.collect { event ->
                    overlayProgress.snapTo(1f - event.progress)
                }
                if (animatorsEnabled) {
                    overlayProgress.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                } else {
                    overlayProgress.snapTo(0f)
                }
                closeTopOverlayAction()
            } catch (_: CancellationException) {
                overlayProgress.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
            }
        }

        // Inner sub-state back handlers for Instagram media/details before exiting chat
        if (activeMessagesPlatform == "instagram" && (igExpandedMediaList != null || igIsViewingDetails)) {
            BackHandler {
                closeTopOverlayAction()
            }
        }

        // Always return to Home before exiting when on Storage (1) or Settings (2) root
        BackHandler(enabled = !hasTopOverlay && selectedTab != 0 && !isSettingsSubScreen) {
            onSelectTab(0)
        }

        // File picker for WhatsApp (.txt or .zip)
        val whatsappPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                uri?.let { selectedUri ->
                    val fileName = getFileName(applicationContext, selectedUri)
                    val isZip = isZipUri(applicationContext, selectedUri)
                    if (isZip) {
                        whatsappViewModel.importZipUri(contentResolver, selectedUri, fileName)
                    } else {
                        whatsappViewModel.importChatUri(contentResolver, selectedUri, fileName)
                    }
                    onSetHomePreferredPlatform("whatsapp")
                    onSetActiveMessagesPlatform("whatsapp")
                }
            }
        )

        // File picker for Instagram (.zip export)
        val instagramPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                uri?.let { selectedUri ->
                    val fileName = getFileName(applicationContext, selectedUri)
                    instagramViewModel.importZipUri(contentResolver, selectedUri, fileName)
                    onSetHomePreferredPlatform("instagram")
                    onSetActiveMessagesPlatform("instagram")
                }
            }
        )

        val pagerState = rememberPagerState(initialPage = selectedTab.coerceIn(0, 2), pageCount = { 3 })
        var pagerNavigationTarget by remember { mutableStateOf<Int?>(null) }
        var pagerNavigationRequest by remember { mutableIntStateOf(0) }

        LaunchedEffect(selectedTab, pagerNavigationRequest, animatorsEnabled) {
            val destination = selectedTab.coerceIn(0, 2)
            pagerNavigationTarget = destination
            val request = pagerNavigationRequest
            try {
                if (pagerState.currentPage != destination || pagerState.currentPageOffsetFraction != 0f) {
                    if (animatorsEnabled) {
                        pagerState.animateScrollToPage(
                            page = destination,
                            animationSpec = spring(stiffness = 380f, dampingRatio = 0.86f)
                        )
                    } else {
                        pagerState.scrollToPage(destination)
                    }
                }
            } finally {
                if (request == pagerNavigationRequest) pagerNavigationTarget = null
            }
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { Triple(pagerState.settledPage, pagerNavigationTarget, pagerState.isScrollInProgress) }
                .distinctUntilChanged()
                .drop(1)
                .collect { (page, requested, scrolling) ->
                    if (requested == null && !scrolling) {
                        onSelectTab(page)
                    }
                }
        }

        val pagerGesturesEnabled = !hasTopOverlay && !(selectedTab == 2 && isSettingsSubScreen)
        val dockVisible = !hasTopOverlay && !(selectedTab == 2 && isSettingsSubScreen)
        val dockTab = if (pagerNavigationTarget == null) pagerState.currentPage else selectedTab

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(
                    hostState = confirmations.hostState,
                    modifier = Modifier.padding(bottom = if (dockVisible) WasmMotionTokens.DockReserve else 20.dp)
                ) { data ->
                    WasmConfirmationCapsule(data = data)
                }
            }
        ) { innerPadding ->
            val underDockBottom = innerPadding.calculateBottomPadding() + WasmMotionTokens.DockReserve
            val tabContentPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(ld),
                end = innerPadding.calculateEndPadding(ld),
                top = innerPadding.calculateTopPadding(),
                bottom = underDockBottom
            )

            Box(modifier = Modifier.fillMaxSize()) {
                // Base 3-Tab Horizon Layer (subtle parallax recede when an overlay slides over it)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = overlayProgress.value
                            translationX = -size.width * 0.10f * p
                            alpha = 1f - 0.06f * p
                        }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        beyondBoundsPageCount = 2,
                        key = { it },
                        userScrollEnabled = pagerGesturesEnabled,
                        flingBehavior = PagerDefaults.flingBehavior(
                            state = pagerState,
                            snapAnimationSpec = spring(stiffness = 380f, dampingRatio = 0.86f)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> Box(modifier = Modifier.fillMaxSize().padding(tabContentPadding)) {
                                HomeScreen(
                                    whatsappViewModel = whatsappViewModel,
                                    instagramViewModel = instagramViewModel,
                                    prefs = prefs,
                                    initialPlatform = homePreferredPlatform,
                                    pendingOpenChat = pendingGoToChat,
                                    onPendingOpenChatConsumed = { pendingGoToChat = null },
                                    staggerEntranceToken = staggerEntranceToken,
                                    onStaggerConsumed = onStaggerConsumed,
                                    showEmbeddedFab = false,
                                    onConfirmation = { confirmations.show(it) },
                                    onNavigateToMessages = {
                                        onSetActiveMessagesPlatform(null)
                                        pagerNavigationTarget = 1
                                        pagerNavigationRequest++
                                        onSelectTab(1)
                                    },
                                    onNavigateToSettings = {
                                        onSetActiveMessagesPlatform(null)
                                        pagerNavigationTarget = 2
                                        pagerNavigationRequest++
                                        onSelectTab(2)
                                    },
                                    onOpenImport = { onSetShowImportScreen(true) },
                                    onOpenWhatsApp = { onSetActiveMessagesPlatform("whatsapp") },
                                    onOpenInstagram = { onSetActiveMessagesPlatform("instagram") },
                                    onOpenWhatsAppConversation = { convId, msgId ->
                                        whatsappViewModel.selectConversation(convId, msgId)
                                        onSetActiveMessagesPlatform("whatsapp")
                                    },
                                    onOpenInstagramConversation = { convId, msgId ->
                                        instagramViewModel.selectConversation(convId, msgId)
                                        onSetActiveMessagesPlatform("instagram")
                                    },
                                    onImportWhatsApp = { onSetShowImportScreen(true) },
                                    onImportInstagram = { onSetShowImportScreen(true) }
                                )
                            }
                            1 -> Box(modifier = Modifier.fillMaxSize().padding(tabContentPadding)) {
                                StorageScreen(
                                    whatsappViewModel = whatsappViewModel,
                                    instagramViewModel = instagramViewModel,
                                    prefs = prefs,
                                    onOpenImport = { onSetShowImportScreen(true) },
                                    onNavigateToSettings = {
                                        pagerNavigationTarget = 2
                                        pagerNavigationRequest++
                                        onSelectTab(2)
                                    },
                                    onOpenStorageData = { onSetShowStorageDataModal(true) }
                                )
                            }
                            else -> Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        if (isSettingsSubScreen) innerPadding
                                        else tabContentPadding
                                    )
                            ) {
                                SettingsScreen(
                                    whatsappViewModel = whatsappViewModel,
                                    instagramViewModel = instagramViewModel,
                                    prefs = prefs,
                                    selectedLanguageCode = appLanguage,
                                    onReplayOnboarding = onReplayOnboarding,
                                    onSubScreenChanged = onSetSettingsSubScreen,
                                    onConfirmation = { confirmations.show(it) },
                                    onBackToHome = {
                                        pagerNavigationTarget = 0
                                        pagerNavigationRequest++
                                        onSelectTab(0)
                                    }
                                )
                            }
                        }
                    }

                    // Floating M3 Expressive Dock (live placement indicator tracking + contextual Add FAB)
                    AnimatedVisibility(
                        visible = dockVisible,
                        enter = slideInVertically(
                            animationSpec = spring(stiffness = 380f, dampingRatio = 0.82f)
                        ) { it } + fadeIn(tween(180)),
                        exit = slideOutVertically(
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        ) { it } + fadeOut(tween(140)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = WasmMotionTokens.DockClearance)
                    ) {
                        WasmDock(
                            selectedTab = dockTab,
                            homeLabel = strings.navHome,
                            storageLabel = strings.navStorage,
                            settingsLabel = strings.navSettings,
                            importLabel = strings.importFile,
                            pagePosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                            showImportAction = dockTab == 0,
                            onSelectTab = { nextTab ->
                                pagerNavigationTarget = nextTab
                                pagerNavigationRequest++
                                onSelectTab(nextTab)
                            },
                            onOpenImport = { onSetShowImportScreen(true) }
                        )
                    }
                }

                // Foreground Overlay Stack (Import, Chat, Instagram, Wallpaper, StorageData)
                val overlayKey = currentOverlayKey ?: renderedOverlayKey
                if (overlayKey != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f * overlayProgress.value))
                            .graphicsLayer {
                                val p = overlayProgress.value
                                translationX = size.width * (1f - p)
                                alpha = 0.90f + 0.10f * p
                            }
                    ) {
                        when (overlayKey) {
                            "wallpaper" -> WallpaperScreen(
                                prefs = prefs,
                                onBack = { onSetShowWallpaperModal(false) }
                            )
                            "storage_data" -> StorageDataScreen(
                                whatsappViewModel = whatsappViewModel,
                                instagramViewModel = instagramViewModel,
                                onConfirmation = { confirmations.show(it) },
                                onBack = { onSetShowStorageDataModal(false) }
                            )
                            "import" -> ImportScreen(
                                whatsappViewModel = whatsappViewModel,
                                instagramViewModel = instagramViewModel,
                                onImportComplete = { platform ->
                                    val msg = if (platform == "whatsapp") strings.importedWhatsappSnackbar else strings.importedInstagramSnackbar
                                    confirmations.show(msg)
                                    onSetHomePreferredPlatform(platform)
                                    onSetShowImportScreen(false)
                                    pagerNavigationTarget = 0
                                    pagerNavigationRequest++
                                    onSelectTab(0)
                                },
                                onNavigateToDuplicateChat = { platform, conversationId ->
                                    onSetHomePreferredPlatform(platform)
                                    pendingGoToChat = platform to conversationId
                                    onSetShowImportScreen(false)
                                    pagerNavigationTarget = 0
                                    pagerNavigationRequest++
                                    onSelectTab(0)
                                },
                                onBack = { onSetShowImportScreen(false) }
                            )
                            "whatsapp" -> ChatScreen(
                                viewModel = whatsappViewModel,
                                onImportClick = {
                                    whatsappPickerLauncher.launch(
                                        arrayOf(
                                            "text/plain",
                                            "application/zip",
                                            "application/x-zip-compressed",
                                            "application/octet-stream"
                                        )
                                    )
                                },
                                onBackToPlatformChooser = {
                                    onSetActiveMessagesPlatform(null)
                                    onSelectTab(0)
                                },
                                onCloseConversation = {
                                    whatsappViewModel.closeConversation()
                                    onSetActiveMessagesPlatform(null)
                                    onSelectTab(0)
                                },
                                onOpenWallpaper = { onSetShowWallpaperModal(true) },
                                onConfirmation = { confirmations.show(it) }
                            )
                            "instagram" -> InstagramScreen(
                                viewModel = instagramViewModel,
                                onImportClick = {
                                    instagramPickerLauncher.launch(
                                        arrayOf(
                                            "application/zip",
                                            "application/x-zip-compressed",
                                            "application/octet-stream"
                                        )
                                    )
                                },
                                onBackToPlatformChooser = {
                                    onSetActiveMessagesPlatform(null)
                                    onSelectTab(0)
                                },
                                onCloseConversation = {
                                    instagramViewModel.closeConversation()
                                    onSetActiveMessagesPlatform(null)
                                    onSelectTab(0)
                                },
                                onOpenWallpaper = { onSetShowWallpaperModal(true) },
                                onConfirmation = { confirmations.show(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isZipUri(context: Context, uri: Uri): Boolean {
        val name = getFileName(context, uri)
        val mimeType = context.contentResolver.getType(uri)
        return mimeType == "application/zip" ||
                mimeType == "application/x-zip-compressed" ||
                mimeType == "application/octet-stream" ||
                name?.endsWith(".zip", ignoreCase = true) == true
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }
}

