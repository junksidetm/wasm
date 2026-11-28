package com.mrdartsidetm.wasm

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import com.mrdartsidetm.wasm.ui.PlatformChooserScreen
import com.mrdartsidetm.wasm.ui.SettingsScreen
import com.mrdartsidetm.wasm.ui.StorageDataScreen
import com.mrdartsidetm.wasm.ui.StorageScreen
import com.mrdartsidetm.wasm.ui.WallpaperScreen
import com.mrdartsidetm.wasm.ui.instagram.InstagramScreen
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.localization.languageByCode
import com.mrdartsidetm.wasm.ui.localization.wasmStringsFor
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
                axisConfig = axisConfig
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

                CompositionLocalProvider(
                    LocalWasmStrings provides strings,
                    LocalLayoutDirection provides layoutDirection,
                    LocalChatWallpaper provides ChatWallpaperConfig(chatWallpaper, chatWallpaperImagePath)
                ) {
                    when {
                        onboardingCompleted == null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(OnboardingBgBottom)
                            )
                        }
                        onboardingCompleted == false || forceShowOnboarding -> {
                            OnboardingFlow(
                                whatsappViewModel = whatsappViewModel,
                                instagramViewModel = instagramViewModel,
                                prefs = prefs,
                                selectedLanguageCode = appLanguage,
                                onFinishOnboarding = { importedPlatform ->
                                    coroutineScope.launch {
                                        prefs.setOnboardingCompleted(true)
                                    }
                                    forceShowOnboarding = false
                                    if (importedPlatform != null) {
                                        homePreferredPlatform = importedPlatform
                                    }
                                    activeMessagesPlatform = null
                                    showImportScreen = false
                                    selectedTab = 0
                                }
                            )
                        }
                        else -> {

                val waConversations by whatsappViewModel.conversations.collectAsStateWithLifecycle()
                val igConversations by instagramViewModel.conversations.collectAsStateWithLifecycle()
                val igAccount by instagramViewModel.account.collectAsStateWithLifecycle()
                val waSelectedConversationId by whatsappViewModel.selectedConversationId.collectAsStateWithLifecycle()
                val igSelectedConversationId by instagramViewModel.selectedConversationId.collectAsStateWithLifecycle()
                val igIsViewingDetails by instagramViewModel.isViewingConversationDetails.collectAsStateWithLifecycle()
                val igExpandedMediaList by instagramViewModel.activeExpandedMediaList.collectAsStateWithLifecycle()

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
                            homePreferredPlatform = "whatsapp"
                            activeMessagesPlatform = "whatsapp"
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
                            homePreferredPlatform = "instagram"
                            activeMessagesPlatform = "instagram"
                        }
                    }
                )

                // System BackPress Handler
                when {
                    showWallpaperModal -> BackHandler { showWallpaperModal = false }
                    showStorageDataModal -> BackHandler { showStorageDataModal = false }
                    activeMessagesPlatform == "whatsapp" -> {
                        if (waSelectedConversationId != null) {
                            BackHandler {
                                whatsappViewModel.closeConversation()
                                activeMessagesPlatform = null
                                selectedTab = 0
                            }
                        } else {
                            BackHandler {
                                activeMessagesPlatform = null
                                selectedTab = 0
                            }
                        }
                    }
                    activeMessagesPlatform == "instagram" -> {
                        when {
                            igExpandedMediaList != null -> BackHandler { instagramViewModel.closeExpandedMedia() }
                            igIsViewingDetails -> BackHandler { instagramViewModel.closeConversationDetails() }
                            igSelectedConversationId != null -> BackHandler {
                                instagramViewModel.closeConversation()
                                activeMessagesPlatform = null
                                selectedTab = 0
                            }
                            else -> BackHandler {
                                activeMessagesPlatform = null
                                selectedTab = 0
                            }
                        }
                    }
                    selectedTab != 0 && !showImportScreen && !isSettingsSubScreen -> {
                        BackHandler {
                            selectedTab = 0
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (!showImportScreen && !showWallpaperModal && !showStorageDataModal && activeMessagesPlatform == null && !(selectedTab == 2 && isSettingsSubScreen)) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = strings.navHome
                                        )
                                    },
                                    label = { Text(strings.navHome) }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == 1) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                                            contentDescription = strings.navStorage
                                        )
                                    },
                                    label = { Text(strings.navStorage) }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                                            contentDescription = strings.navSettings
                                        )
                                    },
                                    label = { Text(strings.navSettings) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when {
                            showWallpaperModal -> WallpaperScreen(
                                prefs = prefs,
                                onBack = { showWallpaperModal = false }
                            )
                            showStorageDataModal -> StorageDataScreen(
                                whatsappViewModel = whatsappViewModel,
                                instagramViewModel = instagramViewModel,
                                onBack = { showStorageDataModal = false }
                            )
                            showImportScreen -> ImportScreen(
                                whatsappViewModel = whatsappViewModel,
                                instagramViewModel = instagramViewModel,
                                onImportComplete = { platform ->
                                    homePreferredPlatform = platform
                                    showImportScreen = false
                                    selectedTab = 0
                                },
                                onBack = { showImportScreen = false }
                            )
                            activeMessagesPlatform == "whatsapp" -> ChatScreen(
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
                                    activeMessagesPlatform = null
                                    selectedTab = 0
                                },
                                onCloseConversation = {
                                    whatsappViewModel.closeConversation()
                                    activeMessagesPlatform = null
                                    selectedTab = 0
                                },
                                onOpenWallpaper = { showWallpaperModal = true }
                            )
                            activeMessagesPlatform == "instagram" -> InstagramScreen(
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
                                    activeMessagesPlatform = null
                                    selectedTab = 0
                                },
                                onCloseConversation = {
                                    instagramViewModel.closeConversation()
                                    activeMessagesPlatform = null
                                    selectedTab = 0
                                },
                                onOpenWallpaper = { showWallpaperModal = true }
                            )
                            selectedTab == 0 -> HomeScreen(
                                whatsappViewModel = whatsappViewModel,
                                instagramViewModel = instagramViewModel,
                                prefs = prefs,
                                initialPlatform = homePreferredPlatform,
                                onNavigateToMessages = {
                                    activeMessagesPlatform = null
                                    selectedTab = 1
                                },
                                onNavigateToSettings = {
                                    activeMessagesPlatform = null
                                    selectedTab = 2
                                },
                                onOpenImport = {
                                    showImportScreen = true
                                },
                                onOpenWhatsApp = {
                                    activeMessagesPlatform = "whatsapp"
                                },
                                onOpenInstagram = {
                                    activeMessagesPlatform = "instagram"
                                },
                                onOpenWhatsAppConversation = { convId, msgId ->
                                    whatsappViewModel.selectConversation(convId, msgId)
                                    activeMessagesPlatform = "whatsapp"
                                },
                                onOpenInstagramConversation = { convId, msgId ->
                                    instagramViewModel.selectConversation(convId, msgId)
                                    activeMessagesPlatform = "instagram"
                                },
                                onImportWhatsApp = {
                                    showImportScreen = true
                                },
                                onImportInstagram = {
                                    showImportScreen = true
                                }
                            )
                            selectedTab == 1 -> StorageScreen(
                                whatsappViewModel = whatsappViewModel,
                                instagramViewModel = instagramViewModel,
                                prefs = prefs,
                                onOpenImport = { showImportScreen = true },
                                onNavigateToSettings = { selectedTab = 2 },
                                onOpenStorageData = { showStorageDataModal = true }
                            )
                            else -> SettingsScreen(
                                whatsappViewModel = whatsappViewModel,
                                instagramViewModel = instagramViewModel,
                                prefs = prefs,
                                selectedLanguageCode = appLanguage,
                                onReplayOnboarding = { forceShowOnboarding = true },
                                onSubScreenChanged = { isSettingsSubScreen = it },
                                onBackToHome = { selectedTab = 0 }
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

