package com.mrdartsidetm.wasm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.R
import com.mrdartsidetm.wasm.data.UserPreferencesRepository
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.instagram.getAvatarColor
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.motion.WasmMechanicalText
import com.mrdartsidetm.wasm.ui.motion.WasmMotionTokens
import com.mrdartsidetm.wasm.ui.motion.performWasmTickHaptic
import com.mrdartsidetm.wasm.ui.motion.rememberAnimatorsEnabled
import com.mrdartsidetm.wasm.ui.motion.rememberLazyListHeaderFalloff
import com.mrdartsidetm.wasm.ui.motion.wasmFeatheredVerticalViewport
import com.mrdartsidetm.wasm.ui.motion.wasmLuminousElasticClickable
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding535
import com.mrdartsidetm.wasm.ui.theme.GoogleSansFlexOnboarding875
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private const val ARCHIVE_ICON_BOX_PATH =
    "M 27.629 0.004 C 28.272 0.049 28.848 0.431 29.14 1.013 L 32.806 8.347 C 32.809 8.354 32.812 8.361 32.815 8.368 C 32.933 8.61 33 8.88 33 9.167 V 27.5 C 33 30.537 30.537 33 27.5 33 H 25.666 C 24.654 33 23.833 32.179 23.833 31.167 C 23.833 30.155 24.654 29.334 25.666 29.334 H 27.5 C 28.512 29.334 29.333 28.512 29.333 27.5 V 11 H 3.666 V 27.5 C 3.666 28.512 4.488 29.334 5.5 29.334 H 7.333 C 8.346 29.334 9.166 30.154 9.166 31.167 C 9.166 32.179 8.345 33 7.333 33 H 5.5 C 2.463 33 0 30.537 0 27.5 V 9.167 C 0 9.135 0.003 9.103 0.005 9.071 C 0.006 9.047 0.006 9.022 0.008 8.998 C 0.021 8.856 0.05 8.719 0.094 8.589 C 0.121 8.507 0.154 8.425 0.193 8.347 L 3.86 1.013 L 3.922 0.899 C 4.25 0.345 4.849 0 5.5 0 H 27.5 L 27.629 0.004 Z M 4.8 7.333 H 28.2 L 26.366 3.666 H 6.633 L 4.8 7.333 Z"

private const val ARCHIVE_ICON_ARROW_PATH =
    "M 16.5 18.334 C 17.512 18.334 18.334 19.155 18.334 20.167 V 26.74 L 19.787 25.287 C 20.503 24.571 21.664 24.571 22.38 25.287 C 23.096 26.003 23.096 27.164 22.38 27.88 L 17.797 32.463 C 17.453 32.807 16.986 33 16.5 33 C 16.014 33 15.548 32.806 15.204 32.463 L 10.621 27.88 C 9.905 27.164 9.905 26.003 10.621 25.287 C 11.337 24.572 12.497 24.572 13.213 25.287 L 14.667 26.741 V 20.167 C 14.667 19.155 15.488 18.334 16.5 18.334 Z"

private enum class HomePlatformTab {
    WHATSAPP,
    INSTAGRAM
}

private data class HomeChatRowUi(
    val key: String,
    val conversationId: String,
    val title: String,
    val supportingText: String,
    val timestampText: String,
    val isArchived: Boolean,
    val avatarPath: String? = null,
    val targetMessageIdWa: Int? = null,
    val targetMessageIdIg: Long? = null,
    val isMessageMatch: Boolean = false
)

/**
 * Material 3 Expressive Home Screen built from the categorized wireframe:
 * - Top Section: "WASM" wordmark + overflow ⋮ menu (or Archive sub-view header)
 * - Top Middle Section: Full-pill Search Bar ("Search (Names, dates, message)") + Connected Button Group (Whatsapp | Instagram)
 * - Archive Section: Rounded Archive banner with live archived count for the active platform
 * - Middle Section: Calibrated M3 Expressive connected chat list supporting search-to-message jump & long-press archiving
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    whatsappViewModel: ChatViewModel,
    instagramViewModel: InstagramViewModel,
    prefs: UserPreferencesRepository,
    initialPlatform: String? = null,
    pendingOpenChat: Pair<String, String>? = null,
    onPendingOpenChatConsumed: () -> Unit = {},
    staggerEntranceToken: Int = 0,
    onStaggerConsumed: () -> Unit = {},
    showEmbeddedFab: Boolean = true,
    onConfirmation: (String) -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onOpenImport: () -> Unit = {},
    onOpenWhatsApp: () -> Unit = {},
    onOpenInstagram: () -> Unit = {},
    onOpenWhatsAppConversation: (String, Int?) -> Unit = { _, _ -> },
    onOpenInstagramConversation: (String, Long?) -> Unit = { _, _ -> },
    onImportWhatsApp: () -> Unit = {},
    onImportInstagram: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalWasmStrings.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val animatorsEnabled = rememberAnimatorsEnabled()
    val isArchiveHidden by prefs.isArchiveHidden.collectAsStateWithLifecycle(initialValue = false)

    var selectedPlatform by rememberSaveable {
        mutableStateOf(
            if (initialPlatform == "instagram") HomePlatformTab.INSTAGRAM else HomePlatformTab.WHATSAPP
        )
    }
    LaunchedEffect(initialPlatform) {
        if (initialPlatform == "instagram") {
            selectedPlatform = HomePlatformTab.INSTAGRAM
        } else if (initialPlatform == "whatsapp") {
            selectedPlatform = HomePlatformTab.WHATSAPP
        }
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isViewingArchive by rememberSaveable { mutableStateOf(false) }
    var isPersonalizeOpen by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val waConversations by whatsappViewModel.conversations.collectAsStateWithLifecycle()
    val igConversations by instagramViewModel.conversations.collectAsStateWithLifecycle()
    val waMatchingMessages by whatsappViewModel.homeMatchingMessages.collectAsStateWithLifecycle()
    val igMatchingMessages by instagramViewModel.homeMatchingMessages.collectAsStateWithLifecycle()

    LaunchedEffect(searchQuery, selectedPlatform) {
        whatsappViewModel.setHomeSearchQuery(if (selectedPlatform == HomePlatformTab.WHATSAPP) searchQuery else "")
        instagramViewModel.setHomeSearchQuery(if (selectedPlatform == HomePlatformTab.INSTAGRAM) searchQuery else "")
    }

    if (isViewingArchive) {
        BackHandler {
            isViewingArchive = false
        }
    }

    val waTitleMap = remember(waConversations) { waConversations.associateBy { it.id } }
    val igTitleMap = remember(igConversations) { igConversations.associateBy { it.id } }

    val archivedCount = remember(selectedPlatform, waConversations, igConversations) {
        when (selectedPlatform) {
            HomePlatformTab.WHATSAPP -> waConversations.count { it.isArchived }
            HomePlatformTab.INSTAGRAM -> igConversations.count { it.isArchived }
        }
    }

    val displayedRows: List<HomeChatRowUi> = remember(
        selectedPlatform,
        searchQuery,
        isViewingArchive,
        waConversations,
        igConversations,
        waMatchingMessages,
        igMatchingMessages,
        waTitleMap,
        igTitleMap
    ) {
        val q = searchQuery.trim().lowercase()
        when (selectedPlatform) {
            HomePlatformTab.WHATSAPP -> {
                if (q.isNotBlank()) {
                    val convHits = waConversations
                        .filter {
                            (!isViewingArchive || it.isArchived) && (
                                it.title.lowercase().contains(q) ||
                                    it.lastTimestamp.lowercase().contains(q) ||
                                    it.lastMessage.lowercase().contains(q)
                                )
                        }
                        .map { conv ->
                            HomeChatRowUi(
                                key = "wa_conv_${conv.id}",
                                conversationId = conv.id,
                                title = conv.title,
                                supportingText = conv.lastMessage.ifBlank { conv.lastTimestamp },
                                timestampText = conv.lastTimestamp,
                                isArchived = conv.isArchived,
                                avatarPath = conv.avatarPath
                            )
                        }
                    val msgHits = waMatchingMessages
                        .filter { msg ->
                            val conv = waTitleMap[msg.conversationId]
                            conv != null && (!isViewingArchive || conv.isArchived)
                        }
                        .map { msg ->
                            val conv = waTitleMap[msg.conversationId]!!
                            val preview = if (msg.sender.isNotBlank() && !msg.isSystemMessage) {
                                "${msg.sender}: ${msg.content}"
                            } else {
                                msg.content
                            }
                            HomeChatRowUi(
                                key = "wa_msg_${msg.id}",
                                conversationId = msg.conversationId,
                                title = conv.title,
                                supportingText = preview,
                                timestampText = msg.timestamp,
                                isArchived = conv.isArchived,
                                avatarPath = conv.avatarPath,
                                targetMessageIdWa = msg.id,
                                isMessageMatch = true
                            )
                        }
                    convHits + msgHits
                } else {
                    waConversations
                        .filter { it.isArchived == isViewingArchive }
                        .map { conv ->
                            HomeChatRowUi(
                                key = "wa_conv_${conv.id}",
                                conversationId = conv.id,
                                title = conv.title,
                                supportingText = conv.lastMessage.ifBlank { "${conv.messageCount} messages" },
                                timestampText = conv.lastTimestamp,
                                isArchived = conv.isArchived,
                                avatarPath = conv.avatarPath
                            )
                        }
                }
            }
            HomePlatformTab.INSTAGRAM -> {
                if (q.isNotBlank()) {
                    val convHits = igConversations
                        .filter {
                            (!isViewingArchive || it.isArchived) && (
                                it.title.lowercase().contains(q) ||
                                    it.lastTimestamp.lowercase().contains(q) ||
                                    it.lastMessage.lowercase().contains(q)
                                )
                        }
                        .map { conv ->
                            HomeChatRowUi(
                                key = "ig_conv_${conv.id}",
                                conversationId = conv.id,
                                title = conv.title,
                                supportingText = conv.lastMessage.ifBlank { conv.lastTimestamp },
                                timestampText = conv.lastTimestamp,
                                isArchived = conv.isArchived,
                                avatarPath = conv.avatarPath
                            )
                        }
                    val msgHits = igMatchingMessages
                        .filter { msg ->
                            val conv = igTitleMap[msg.conversationId]
                            conv != null && (!isViewingArchive || conv.isArchived)
                        }
                        .map { msg ->
                            val conv = igTitleMap[msg.conversationId]!!
                            val body = msg.content.ifBlank { msg.sharedTitle ?: msg.mediaType ?: "" }
                            val preview = if (msg.sender.isNotBlank()) "${msg.sender}: $body" else body
                            HomeChatRowUi(
                                key = "ig_msg_${msg.id}",
                                conversationId = msg.conversationId,
                                title = conv.title,
                                supportingText = preview,
                                timestampText = msg.timestamp,
                                isArchived = conv.isArchived,
                                avatarPath = conv.avatarPath,
                                targetMessageIdIg = msg.id,
                                isMessageMatch = true
                            )
                        }
                    convHits + msgHits
                } else {
                    igConversations
                        .filter { it.isArchived == isViewingArchive }
                        .map { conv ->
                            HomeChatRowUi(
                                key = "ig_conv_${conv.id}",
                                conversationId = conv.id,
                                title = conv.title,
                                supportingText = conv.lastMessage.ifBlank { "${conv.messageCount} messages" },
                                timestampText = conv.lastTimestamp,
                                isArchived = conv.isArchived,
                                avatarPath = conv.avatarPath
                            )
                        }
                }
            }
        }
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

            // =========================================================================
            // TOP SECTION: "WASM" Wordmark + ⋮ Overflow Menu (or Archive Sub-header)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isViewingArchive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { isViewingArchive = false },
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
                            text = strings.archiveLabel,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = GoogleSansFlexOnboarding535,
                                fontWeight = FontWeight(535),
                                fontSize = 26.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        text = "WASM",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = GoogleSansFlexOnboarding875,
                            fontWeight = FontWeight(875),
                            fontSize = 30.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = strings.settingsTitle,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Import ${strings.whatsapp}",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_nav_whatsapp),
                                    contentDescription = null,
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onImportWhatsApp()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Import ${strings.instagram}",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_nav_instagram),
                                    contentDescription = null,
                                    tint = Color(0xFFE1306C),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onImportInstagram()
                            }
                        )
                        if (isArchiveHidden) {
                            DropdownMenuItem(
                                text = { Text("Show Archive", maxLines = 1) },
                                leadingIcon = {
                                    Icon(Icons.Default.Unarchive, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    scope.launch { prefs.saveArchiveHidden(false) }
                                }
                            )
                        }
                        Divider()
                        DropdownMenuItem(
                            text = { Text(strings.personalizeProfile, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            onClick = {
                                showOverflowMenu = false
                                isPersonalizeOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.settingsTitle, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onNavigateToSettings()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // TOP MIDDLE SECTION: Search Pill ("Search (Names, dates, message)")
            // =========================================================================
            HomeSearchPill(
                query = searchQuery,
                placeholder = strings.searchNamesDatesMessage,
                onQueryChange = { searchQuery = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // TOP MIDDLE SECTION: Connected Button Group (Whatsapp | Instagram)
            // Hidden when viewing Archive
            // =========================================================================
            AnimatedVisibility(visible = !isViewingArchive) {
                Column {
                    HomeConnectedPlatformToggle(
                        selectedTab = selectedPlatform,
                        whatsappLabel = strings.whatsapp,
                        instagramLabel = strings.instagram,
                        onSelectTab = { tab -> selectedPlatform = tab }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // =========================================================================
            // ARCHIVE SECTION: Rounded Banner with Archive Icon (0.5x) + Count
            // Long-press to hide; unhide via Home 3-dots -> Show Archive
            // =========================================================================
            AnimatedVisibility(visible = !isViewingArchive && !isArchiveHidden) {
                Column {
                    HomeArchiveBanner(
                        title = strings.archiveLabel,
                        archivedCount = archivedCount,
                        onClick = { isViewingArchive = true },
                        onHideArchive = {
                            scope.launch { prefs.saveArchiveHidden(true) }
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // =========================================================================
            // MIDDLE SECTION: Material 3 Expressive Connected Chat List
            // =========================================================================
            val listState = rememberLazyListState()
            val headerFalloff by rememberLazyListHeaderFalloff(listState)
            var exitingRowKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
            var highlightedRowKey by remember { mutableStateOf<String?>(null) }

            // Choreography for "Go to that chat" on duplicate import detection:
            // Close Import -> Land on Home -> Switch pill toggle -> Scroll to row -> Pulse highlight -> Slide open chat
            LaunchedEffect(pendingOpenChat, waConversations, igConversations) {
                val (targetPlatform, targetConvId) = pendingOpenChat ?: return@LaunchedEffect
                val targetTab = if (targetPlatform == "instagram") HomePlatformTab.INSTAGRAM else HomePlatformTab.WHATSAPP
                val targetPrefix = if (targetPlatform == "instagram") "ig_conv_" else "wa_conv_"
                val targetKey = "$targetPrefix$targetConvId"
                val isTargetArchived = if (targetTab == HomePlatformTab.WHATSAPP) {
                    waConversations.firstOrNull { it.id == targetConvId }?.isArchived == true
                } else {
                    igConversations.firstOrNull { it.id == targetConvId }?.isArchived == true
                }

                delay(240) // Allow ImportScreen exit animation to reveal Home
                searchQuery = ""
                if (selectedPlatform != targetTab) {
                    selectedPlatform = targetTab
                    delay(220)
                }
                if (isViewingArchive != isTargetArchived) {
                    isViewingArchive = isTargetArchived
                    delay(180)
                }

                val currentList = if (targetTab == HomePlatformTab.WHATSAPP) {
                    waConversations.filter { it.isArchived == isTargetArchived }.map { "wa_conv_${it.id}" }
                } else {
                    igConversations.filter { it.isArchived == isTargetArchived }.map { "ig_conv_${it.id}" }
                }
                val targetIdx = currentList.indexOf(targetKey)
                if (targetIdx >= 0) {
                    listState.animateScrollToItem(targetIdx)
                }
                highlightedRowKey = targetKey
                view.performWasmTickHaptic(true)
                delay(420)
                highlightedRowKey = null
                onPendingOpenChatConsumed()

                if (targetPlatform == "whatsapp") {
                    onOpenWhatsAppConversation(targetConvId, null)
                } else {
                    onOpenInstagramConversation(targetConvId, null)
                }
            }
            var visibleCardsCount by remember(selectedPlatform, isViewingArchive) {
                mutableIntStateOf(
                    if (staggerEntranceToken > 0 && animatorsEnabled) 0 else Int.MAX_VALUE
                )
            }

            LaunchedEffect(staggerEntranceToken, displayedRows.size, selectedPlatform, isViewingArchive, animatorsEnabled) {
                if (staggerEntranceToken > 0 && animatorsEnabled) {
                    val count = displayedRows.size.coerceAtMost(WasmMotionTokens.StaggerMaxItems)
                    delay(90)
                    for (i in 1..count) {
                        visibleCardsCount = i
                        delay(WasmMotionTokens.StaggerCardDelayMs)
                    }
                    visibleCardsCount = Int.MAX_VALUE
                    onStaggerConsumed()
                } else {
                    visibleCardsCount = Int.MAX_VALUE
                }
            }

            AnimatedContent(
                targetState = selectedPlatform to isViewingArchive,
                transitionSpec = {
                    if (!animatorsEnabled) {
                        fadeIn(tween(140)) togetherWith fadeOut(tween(110))
                    } else if (targetState.second != initialState.second) {
                        val enteringArchive = targetState.second
                        (slideInVertically(
                            animationSpec = spring(stiffness = 380f, dampingRatio = 0.84f)
                        ) { h -> if (enteringArchive) h / 12 else -h / 12 } + fadeIn(tween(200))) togetherWith
                            (fadeOut(tween(150)))
                    } else {
                        val toRight = targetState.first.ordinal > initialState.first.ordinal
                        val dir = if (toRight) 1 else -1
                        (slideInHorizontally(
                            animationSpec = spring(stiffness = 380f, dampingRatio = 0.84f),
                            initialOffsetX = { w -> dir * (w / 5) }
                        ) + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(170, easing = FastOutSlowInEasing),
                                targetOffsetX = { w -> -dir * (w / 6) }
                            ) + fadeOut(tween(150)))
                    }
                },
                label = "HomeChatFeedTransition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { _ ->
                if (displayedRows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when {
                                            searchQuery.isNotBlank() -> Icons.Default.SearchOff
                                            isViewingArchive -> Icons.Default.Inventory2
                                            else -> Icons.Default.Forum
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                text = when {
                                    searchQuery.isNotBlank() -> strings.noSearchMatches
                                    isViewingArchive -> strings.archivedChatsEmpty
                                    else -> strings.noChatsImportedYet
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isBlank() && !isViewingArchive) {
                                Button(
                                    onClick = onOpenImport,
                                    shape = CircleShape,
                                    modifier = Modifier.wasmLuminousElasticClickable(
                                        shape = CircleShape,
                                        onClick = onOpenImport
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = strings.importFile,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .wasmFeatheredVerticalViewport(
                                active = listState.canScrollForward || listState.canScrollBackward,
                                topFadeFraction = 0.025f * (0.4f + 0.6f * headerFalloff),
                                bottomFadeFraction = 0.055f
                            ),
                        contentPadding = PaddingValues(bottom = if (showEmbeddedFab) 88.dp else 20.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(
                            items = displayedRows,
                            key = { _, item -> item.key }
                        ) { index, row ->
                            val shape = connectedListItemShape(
                                index = index,
                                totalCount = displayedRows.size
                            )
                            val rowVisible = row.key !in exitingRowKeys && index < visibleCardsCount
                            AnimatedVisibility(
                                visible = rowVisible,
                                enter = fadeIn(tween(180)) +
                                    scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = spring(dampingRatio = 0.68f, stiffness = 400f)
                                    ) +
                                    slideInVertically(
                                        animationSpec = spring(dampingRatio = 0.68f, stiffness = 400f)
                                    ) { it / 8 },
                                exit = fadeOut(tween(180)) +
                                    scaleOut(
                                        targetScale = 0.92f,
                                        animationSpec = tween(180)
                                    ),
                                modifier = Modifier.animateItemPlacement(WasmMotionTokens.SpringListPlacement)
                            ) {
                                HomeConnectedChatListItem(
                                    row = row,
                                    isWhatsApp = selectedPlatform == HomePlatformTab.WHATSAPP,
                                    isHighlighted = row.key == highlightedRowKey,
                                    shape = shape,
                                    archiveLabel = strings.archiveChat,
                                    unarchiveLabel = strings.unarchiveChat,
                                    openChatLabel = strings.openChat,
                                    onClick = {
                                        if (selectedPlatform == HomePlatformTab.WHATSAPP) {
                                            onOpenWhatsAppConversation(row.conversationId, row.targetMessageIdWa)
                                        } else {
                                            onOpenInstagramConversation(row.conversationId, row.targetMessageIdIg)
                                        }
                                    },
                                    onToggleArchive = {
                                        val nextArchived = !row.isArchived
                                        view.performWasmTickHaptic(!nextArchived)
                                        scope.launch {
                                            if (animatorsEnabled) {
                                                exitingRowKeys = exitingRowKeys + row.key
                                                delay(180)
                                            }
                                            if (selectedPlatform == HomePlatformTab.WHATSAPP) {
                                                whatsappViewModel.setConversationArchived(row.conversationId, nextArchived)
                                            } else {
                                                instagramViewModel.setConversationArchived(row.conversationId, nextArchived)
                                            }
                                            exitingRowKeys = exitingRowKeys - row.key
                                            onConfirmation(if (nextArchived) strings.archiveChat else strings.unarchiveChat)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Material 3 Expressive FAB -> Hidden when the parent WasmDock hosts the contextual Add button
        if (showEmbeddedFab) {
            FloatingActionButton(
                onClick = onOpenImport,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.importFile,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Top-to-Bottom Animated Personalize Page Overlay
        AnimatedVisibility(
            visible = isPersonalizeOpen,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(400)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(350))
        ) {
            PersonalizeScreen(
                prefs = prefs,
                onDismiss = { isPersonalizeOpen = false },
                onNavigateToSettings = {
                    isPersonalizeOpen = false
                    onNavigateToSettings()
                }
            )
        }
    }
}

@Composable
private fun HomeSearchPill(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit
) {
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
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Public string-keyed Connected Button Group for Whatsapp | Instagram selection.
 */
@Composable
fun HomeConnectedPlatformToggle(
    activePlatform: String,
    onSelectPlatform: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalWasmStrings.current
    HomeConnectedPlatformToggle(
        selectedTab = if (activePlatform == "instagram") HomePlatformTab.INSTAGRAM else HomePlatformTab.WHATSAPP,
        whatsappLabel = strings.whatsapp,
        instagramLabel = strings.instagram,
        onSelectTab = { tab ->
            onSelectPlatform(if (tab == HomePlatformTab.INSTAGRAM) "instagram" else "whatsapp")
        },
        modifier = modifier
    )
}

@Composable
private fun HomeConnectedPlatformToggle(
    selectedTab: HomePlatformTab,
    whatsappLabel: String,
    instagramLabel: String,
    onSelectTab: (HomePlatformTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ConnectedSegmentPill(
            label = whatsappLabel,
            iconResId = R.drawable.ic_nav_whatsapp,
            selected = selectedTab == HomePlatformTab.WHATSAPP,
            isLeadingSegment = true,
            onClick = { onSelectTab(HomePlatformTab.WHATSAPP) },
            modifier = Modifier.weight(1f)
        )
        ConnectedSegmentPill(
            label = instagramLabel,
            iconResId = R.drawable.ic_nav_instagram,
            selected = selectedTab == HomePlatformTab.INSTAGRAM,
            isLeadingSegment = false,
            onClick = { onSelectTab(HomePlatformTab.INSTAGRAM) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ConnectedSegmentPill(
    label: String,
    iconResId: Int,
    selected: Boolean,
    isLeadingSegment: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(220),
        label = "SegmentContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220),
        label = "SegmentContent"
    )
    val innerCorner by animateDpAsState(
        targetValue = if (selected) 20.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SegmentInnerCorner"
    )

    val shape = if (isLeadingSegment) {
        RoundedCornerShape(
            topStart = 20.dp,
            bottomStart = 20.dp,
            topEnd = innerCorner,
            bottomEnd = innerCorner
        )
    } else {
        RoundedCornerShape(
            topStart = innerCorner,
            bottomStart = innerCorner,
            topEnd = 20.dp,
            bottomEnd = 20.dp
        )
    }

    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun HomeArchiveBanner(
    title: String,
    archivedCount: Int,
    onClick: () -> Unit,
    onHideArchive: () -> Unit
) {
    val view = LocalView.current
    val boxPath = remember { PathParser().parsePathString(ARCHIVE_ICON_BOX_PATH).toPath() }
    val arrowPath = remember { PathParser().parsePathString(ARCHIVE_ICON_ARROW_PATH).toPath() }
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .wasmLuminousElasticClickable(
                shape = RoundedCornerShape(20.dp),
                maxStretchPercent = 0.025f,
                pressedScale = 0.975f,
                onClick = onClick,
                onLongClick = {
                    view.performWasmTickHaptic(false)
                    onHideArchive()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(22.dp)) {
                    val scale = min(size.width / 33f, size.height / 33f)
                    val dx = (size.width - 33f * scale) / 2f
                    val dy = (size.height - 33f * scale) / 2f
                    withTransform({
                        translate(dx, dy)
                        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                    }) {
                        drawPath(path = boxPath, color = iconColor)
                        drawPath(path = arrowPath, color = iconColor)
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            WasmMechanicalText(
                text = archivedCount.toString(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun connectedListItemShape(index: Int, totalCount: Int): RoundedCornerShape {
    val outer = 20.dp
    val inner = 6.dp
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

@Composable
private fun HomeConnectedChatListItem(
    row: HomeChatRowUi,
    isWhatsApp: Boolean,
    isHighlighted: Boolean = false,
    shape: RoundedCornerShape,
    archiveLabel: String,
    unarchiveLabel: String,
    openChatLabel: String,
    onClick: () -> Unit,
    onToggleArchive: () -> Unit
) {
    val view = LocalView.current
    var showContextMenu by remember { mutableStateOf(false) }
    val itemBgColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(220),
        label = "homeRowHighlight"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = shape,
            color = itemBgColor,
            border = if (isHighlighted) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
                .fillMaxWidth()
                .wasmLuminousElasticClickable(
                    shape = shape,
                    maxStretchPercent = 0.025f,
                    pressedScale = 0.978f,
                    onClick = onClick,
                    onLongClick = {
                        view.performWasmTickHaptic(true)
                        showContextMenu = true
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ConversationAvatar(
                    title = row.title,
                    avatarPath = row.avatarPath,
                    fallbackColor = if (isWhatsApp) getWhatsAppAvatarColor(row.title) else getAvatarColor(row.title),
                    modifier = Modifier.size(42.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = row.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = row.supportingText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (row.timestampText.isNotBlank()) {
                    Text(
                        text = row.timestampText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(openChatLabel, maxLines = 1) },
                leadingIcon = { Icon(Icons.Default.Forum, contentDescription = null) },
                onClick = {
                    showContextMenu = false
                    onClick()
                }
            )
            DropdownMenuItem(
                text = { Text(if (row.isArchived) unarchiveLabel else archiveLabel, maxLines = 1) },
                leadingIcon = {
                    Icon(
                        imageVector = if (row.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = null
                    )
                },
                onClick = {
                    showContextMenu = false
                    onToggleArchive()
                }
            )
        }
    }
}
