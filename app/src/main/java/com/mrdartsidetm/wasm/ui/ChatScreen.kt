package com.mrdartsidetm.wasm.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.R
import com.mrdartsidetm.wasm.ui.components.ChatSearchFilterBar
import com.mrdartsidetm.wasm.ui.components.WasmAvatarCropperDialog
import com.mrdartsidetm.wasm.ui.components.WasmClickableAnnotatedText
import com.mrdartsidetm.wasm.ui.components.WasmExternalFileCard
import com.mrdartsidetm.wasm.ui.components.WasmInlineAudioBubble
import com.mrdartsidetm.wasm.ui.components.WasmInlineStickerOrGif
import com.mrdartsidetm.wasm.ui.components.WasmInlineVideoCard
import com.mrdartsidetm.wasm.ui.components.WasmMediaViewerScreen
import com.mrdartsidetm.wasm.ui.components.WasmViewerMediaItem
import com.mrdartsidetm.wasm.ui.components.WasmViewerMediaType
import com.mrdartsidetm.wasm.ui.motion.wasmElasticClickable
import com.mrdartsidetm.wasm.ui.motion.wasmFeatheredVerticalViewport
import com.mrdartsidetm.wasm.ui.motion.wasmLuminousElasticClickable
import com.mrdartsidetm.wasm.util.AudioPlayerManager
import com.mrdartsidetm.wasm.data.MessageEntity
import com.mrdartsidetm.wasm.data.WhatsAppConversationEntity
import com.mrdartsidetm.wasm.ui.onboarding.ContainedExpressiveLoadingIndicator
import com.mrdartsidetm.wasm.ui.theme.GoogleSansCodeFontFamily
import com.mrdartsidetm.wasm.ui.theme.LocalIsDarkTheme
import com.mrdartsidetm.wasm.ui.theme.LocalWasmMonoFontFamily
import com.mrdartsidetm.wasm.ui.theme.opaqueContainer
import com.mrdartsidetm.wasm.util.ChatRichTextParser
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

// Material 3 Expressive opaque tonal container extensions (fixes translucent layering color bleed)
val ColorScheme.surfaceContainerLowest: Color
    get() = opaqueContainer(0f)

val ColorScheme.surfaceContainerLow: Color
    get() = opaqueContainer(0.28f)

val ColorScheme.surfaceContainer: Color
    get() = opaqueContainer(0.42f)

val ColorScheme.surfaceContainerHigh: Color
    get() = opaqueContainer(0.58f)

val ColorScheme.surfaceContainerHighest: Color
    get() = opaqueContainer(0.78f)

private val WhatsAppAccentGreen = Color(0xFF25D366)

private val AvatarColorsList = listOf(
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF009688),
    Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFFF5722)
)

fun getWhatsAppAvatarColor(name: String): Color {
    val hash = name.hashCode() and 0x7fffffff
    return AvatarColorsList[hash % AvatarColorsList.size]
}

/**
 * Memory-safe LRU Cache for decoded WhatsApp attachment bitmaps.
 * Caches up to 1/8th of available runtime memory to prevent OutOfMemory crashes while maintaining 60-120 FPS.
 */
object BitmapMemoryCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024)
    private val lru = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: ImageBitmap): Int {
            return (bitmap.width * bitmap.height * 4) / 1024
        }
    }

    fun get(key: String): ImageBitmap? = lru.get(key)
    fun put(key: String, bitmap: ImageBitmap) {
        lru.put(key, bitmap)
    }
}

/**
 * Two-pass memory-safe bitmap decoder that calculates inSampleSize power-of-two.
 */
fun decodeSampledBitmap(file: File, reqWidth: Int = 600, reqHeight: Int = 600): ImageBitmap? {
    if (!file.exists() || file.length() == 0L) return null
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
        bitmap?.asImageBitmap()
    } catch (e: Throwable) {
        null
    }
}

/**
 * Formats a file size in bytes to human-readable string (KB, MB).
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    return if (bytes < 1024 * 1024) {
        "${bytes / 1024} KB"
    } else {
        String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
    }
}

/**
 * Extracts the date portion (e.g. "12/05/23" or "12/05/2023") from a timestamp.
 */
fun extractDate(timestamp: String): String {
    val commaIndex = timestamp.indexOf(',')
    return if (commaIndex != -1) {
        timestamp.substring(0, commaIndex).trim()
    } else {
        timestamp
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onImportClick: () -> Unit,
    onBackToPlatformChooser: (() -> Unit)? = null,
    onCloseConversation: (() -> Unit)? = null,
    onOpenWallpaper: () -> Unit = {},
    onConfirmation: (String) -> Unit = {}
) {
    val conversations by viewModel.filteredConversations.collectAsStateWithLifecycle()
    val allConversations by viewModel.conversations.collectAsStateWithLifecycle()
    val selectedConversationId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val messages by viewModel.filteredMessages.collectAsStateWithLifecycle()
    val allMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val senders by viewModel.uniqueSenders.collectAsStateWithLifecycle()
    val isGroupConversation by viewModel.isGroupConversation.collectAsStateWithLifecycle()
    val importState by viewModel.importUiState.collectAsStateWithLifecycle()

    var showIdentityDialog by remember { mutableStateOf(false) }
    var viewerMediaItem by remember { mutableStateOf<WasmViewerMediaItem?>(null) }
    var conversationToDelete by remember { mutableStateOf<WhatsAppConversationEntity?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Auto-prompt identity selection if messages exist but identity is not configured
    LaunchedEffect(senders, currentUser) {
        if (senders.isNotEmpty() && currentUser.isEmpty() && selectedConversationId != null) {
            showIdentityDialog = true
        }
    }

    if (showIdentityDialog && senders.isNotEmpty()) {
        IdentitySelectionDialog(
            senders = senders,
            currentSelected = currentUser,
            onSelected = { selectedName ->
                viewModel.setIdentity(selectedName)
                showIdentityDialog = false
            },
            onDismiss = { showIdentityDialog = false }
        )
    }

    // Single conversation delete confirmation dialog
    conversationToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Chat?") },
            text = { Text("Permanently delete conversation with \"${conv.title}\" and all its messages and media?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(conv.id)
                        conversationToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear all WhatsApp chats confirmation dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All WhatsApp Chats?") },
            text = { Text("This will permanently remove all saved WhatsApp conversations and extracted media files from your local storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllWhatsApp()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dedicated black-background full-screen Photo & Video viewer page
    viewerMediaItem?.let { item ->
        WasmMediaViewerScreen(
            item = item,
            onDismiss = { viewerMediaItem = null },
            onConfirmation = onConfirmation
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedConversationId == null) {
            // Level 1: Conversations List or Empty State
            if (allConversations.isEmpty() && importState !is ImportUiState.Loading) {
                ExpressiveEmptyState(
                    onImportClick = onImportClick,
                    onBack = onBackToPlatformChooser,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                WhatsAppConversationsListScreen(
                    conversations = conversations,
                    allConversations = allConversations,
                    viewModel = viewModel,
                    onConversationClick = { id -> viewModel.selectConversation(id) },
                    onDeleteConversation = { conv -> conversationToDelete = conv },
                    onClearAll = { showClearAllDialog = true },
                    onImportClick = onImportClick,
                    onBack = onBackToPlatformChooser
                )
            }
        } else {
            // Level 2: Individual Chat Detail View
            val chatMediaDir = viewModel.getMediaDirForConversation(activeConversation)
            WhatsAppChatDetailScreen(
                conversation = activeConversation,
                messages = messages,
                allMessages = allMessages,
                currentUser = currentUser,
                senders = senders,
                isGroupChat = isGroupConversation,
                chatMediaDir = chatMediaDir,
                viewModel = viewModel,
                onBack = { (onCloseConversation ?: { viewModel.closeConversation() }).invoke() },
                onSwitchIdentity = { showIdentityDialog = true },
                onOpenWallpaper = onOpenWallpaper,
                onDeleteThisChat = { activeConversation?.let { conversationToDelete = it } },
                onImageClick = { viewerMediaItem = WasmViewerMediaItem(it, WasmViewerMediaType.PHOTO) },
                onVideoClick = { viewerMediaItem = WasmViewerMediaItem(it, WasmViewerMediaType.VIDEO) },
                onConfirmation = onConfirmation
            )
        }

        // Material 3 Expressive Import Progress Banner
        AnimatedVisibility(
            visible = importState is ImportUiState.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            val stepText = (importState as? ImportUiState.Loading)?.step ?: "Importing chat..."
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ContainedExpressiveLoadingIndicator(
                        indicatorColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = stepText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Error Alert Dialog
        (importState as? ImportUiState.Error)?.let { err ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissImportState() },
                icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Import Error") },
                text = { Text(err.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissImportState() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

/**
 * WhatsApp Conversations List Screen (Inbox view showing every individual saved chat).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhatsAppConversationsListScreen(
    conversations: List<WhatsAppConversationEntity>,
    allConversations: List<WhatsAppConversationEntity>,
    viewModel: ChatViewModel,
    onConversationClick: (String) -> Unit,
    onDeleteConversation: (WhatsAppConversationEntity) -> Unit,
    onClearAll: () -> Unit,
    onImportClick: () -> Unit,
    onBack: (() -> Unit)?
) {
    val searchQuery by viewModel.conversationSearchQuery.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    (fadeIn(tween(180)) + slideInVertically { -it / 4 }) togetherWith
                        (fadeOut(tween(140)) + slideOutVertically { -it / 4 })
                },
                label = "waListSearchTopBar"
            ) { searching ->
            if (searching) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setConversationSearchQuery(it) },
                            placeholder = { Text("Search chats...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            viewModel.setConversationSearchQuery("")
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Exit search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setConversationSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back to platform selection")
                            }
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_whatsapp),
                                contentDescription = null,
                                tint = WhatsAppAccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "WhatsApp Chats",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "${allConversations.size} saved ${if (allConversations.size == 1) "chat" else "chats"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search chats")
                        }
                        FilledTonalButton(
                            onClick = onImportClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import", style = MaterialTheme.typography.labelMedium)
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Clear All WhatsApp Chats") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onClearAll()
                                    }
                                )
                            }
                        }
                    }
                )
            }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImportClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Import Chat", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { innerPadding ->
        if (conversations.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No chats found matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    WhatsAppConversationCard(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) },
                        onDelete = { onDeleteConversation(conversation) }
                    )
                }
            }
        }
    }
}

/**
 * Individual WhatsApp conversation card displayed in the conversations list.
 */
@Composable
private fun WhatsAppConversationCard(
    conversation: WhatsAppConversationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wasmLuminousElasticClickable(
                shape = RoundedCornerShape(20.dp),
                maxStretchPercent = 0.025f,
                pressedScale = 0.978f,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle (custom photo if set, else monogram)
            ConversationAvatar(
                title = conversation.title,
                avatarPath = conversation.avatarPath,
                fallbackColor = getWhatsAppAvatarColor(conversation.title),
                modifier = Modifier.size(50.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Conversation info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (conversation.lastTimestamp.isNotBlank()) {
                        Text(
                            text = conversation.lastTimestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (conversation.lastMessage.isNotBlank()) conversation.lastMessage else "No messages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WhatsAppAccentGreen.copy(alpha = 0.16f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${conversation.messageCount} msgs",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = WhatsAppAccentGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Delete single conversation button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Reusable circular avatar displaying a custom picked photo if available, or an initial monogram.
 */
@Composable
fun ConversationAvatar(
    title: String,
    avatarPath: String?,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    val avatarFile = remember(avatarPath) {
        avatarPath?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() }
    }
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = avatarFile?.absolutePath) {
        value = if (avatarFile != null) {
            BitmapMemoryCache.get(avatarFile.absolutePath) ?: withContext(Dispatchers.IO) {
                decodeSampledBitmap(avatarFile, 256, 256)?.also {
                    BitmapMemoryCache.put(avatarFile.absolutePath, it)
                }
            }
        } else {
            null
        }
    }

    val bmp = bitmapState.value
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Surface(
            shape = CircleShape,
            color = fallbackColor,
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
                val initial = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"
                Text(
                    text = initial,
                    style = textStyle,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * WhatsApp Individual Chat Detail Screen (viewing messages of a single selected conversation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhatsAppChatDetailScreen(
    conversation: WhatsAppConversationEntity?,
    messages: List<MessageEntity>,
    allMessages: List<MessageEntity>,
    currentUser: String,
    senders: List<String>,
    isGroupChat: Boolean,
    chatMediaDir: File,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onSwitchIdentity: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onDeleteThisChat: () -> Unit,
    onImageClick: (File) -> Unit,
    onVideoClick: (File) -> Unit,
    onConfirmation: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val dateTimeFilter by viewModel.dateTimeFilter.collectAsStateWithLifecycle()
    val targetMessageId by viewModel.targetMessageId.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val isDark = LocalIsDarkTheme.current
    var initialScrollDoneForId by remember(conversation?.id) { mutableStateOf(false) }

    val showScrollToBottom by remember(messages.size) {
        derivedStateOf {
            messages.isNotEmpty() &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) < messages.lastIndex
        }
    }

    var pendingAvatarCropUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && conversation != null) {
            pendingAvatarCropUri = uri
        }
    }

    pendingAvatarCropUri?.let { cropUri ->
        WasmAvatarCropperDialog(
            imageUri = cropUri,
            onDismiss = { pendingAvatarCropUri = null },
            onCroppedFile = { croppedFile ->
                conversation?.id?.let { convId ->
                    viewModel.updateConversationAvatar(convId, croppedFile)
                    onConfirmation("Avatar updated")
                }
                pendingAvatarCropUri = null
            },
            onError = { msg -> onConfirmation(msg) }
        )
    }

    LaunchedEffect(messages.size, targetMessageId, conversation?.id) {
        if (messages.isEmpty()) return@LaunchedEffect
        val targetId = targetMessageId
        if (targetId != null) {
            val idx = messages.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                listState.scrollToItem(idx)
                initialScrollDoneForId = true
                viewModel.consumeTargetMessage()
            }
        } else if (!initialScrollDoneForId) {
            listState.scrollToItem(messages.lastIndex)
            initialScrollDoneForId = true
        }
    }

    Scaffold(
        containerColor = if (isDark) Color(0xFF0B141A) else Color(0xFFEFEAE2),
        topBar = {
            Column {
                AnimatedContent(
                    targetState = isSearchActive,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically { -it / 4 }) togetherWith
                            (fadeOut(tween(140)) + slideOutVertically { -it / 4 })
                    },
                    label = "waChatSearchTopBar"
                ) { searching ->
                    if (searching) {
                        TopAppBar(
                            title = {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Search in chat...") },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    viewModel.clearSearchFilters()
                                }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Exit search")
                                }
                            },
                            actions = {
                                if (searchQuery.isNotEmpty() || dateTimeFilter.isActive) {
                                    IconButton(onClick = { viewModel.clearSearchFilters() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            }
                        )
                    } else {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to chats")
                                }
                            },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ConversationAvatar(
                                        title = conversation?.title ?: "Chat",
                                        avatarPath = conversation?.avatarPath,
                                        fallbackColor = getWhatsAppAvatarColor(conversation?.title ?: "Chat"),
                                        modifier = Modifier.size(38.dp),
                                        textStyle = MaterialTheme.typography.titleSmall
                                    )
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = conversation?.title ?: "WhatsApp Chat",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isGroupChat && senders.isNotEmpty()) {
                                            Text(
                                                text = "Group · ${senders.size} participants",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                if (allMessages.isNotEmpty()) {
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search messages")
                                    }
                                }
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        if (messages.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Go to top Message") },
                                                leadingIcon = { Icon(Icons.Default.VerticalAlignTop, contentDescription = null) },
                                                onClick = {
                                                    showMenu = false
                                                    scope.launch { listState.animateScrollToItem(0) }
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Change Wallpaper") },
                                            leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                                            onClick = {
                                                showMenu = false
                                                onOpenWallpaper()
                                            }
                                        )
                                        if (senders.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Switch Identity") },
                                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                                onClick = {
                                                    showMenu = false
                                                    onSwitchIdentity()
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Change Avatar Photo") },
                                            leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                            onClick = {
                                                showMenu = false
                                                avatarPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Chat") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.DeleteSweep,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                onDeleteThisChat()
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                AnimatedVisibility(visible = isSearchActive) {
                    ChatSearchFilterBar(
                        filter = dateTimeFilter,
                        resultCount = messages.size,
                        onFilterChange = { viewModel.setDateTimeFilter(it) }
                    )
                }
            }
        }
    ) { innerPadding ->
        ChatWallpaperContainer(
            usePlatformDefaultWhenUnset = true,
            defaultBackgroundColor = if (isDark) Color(0xFF0B141A) else Color(0xFFEFEAE2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                        val currentDate = extractDate(message.timestamp)
                        val prevMessage = if (index > 0) messages[index - 1] else null
                        val prevDate = prevMessage?.let { extractDate(it.timestamp) } ?: ""
                        val isDateBreak = currentDate != prevDate && currentDate.isNotEmpty()

                        if (isDateBreak) {
                            DateSeparatorHeader(date = currentDate)
                        }

                        if (message.isSystemMessage) {
                            SystemMessageChip(content = message.content)
                        } else {
                            val isMe = message.sender == currentUser

                            val isFirstInGroup = prevMessage == null ||
                                prevMessage.isSystemMessage ||
                                prevMessage.sender != message.sender ||
                                isDateBreak

                            val nextMessage = if (index < messages.size - 1) messages[index + 1] else null
                            val nextDate = nextMessage?.let { extractDate(it.timestamp) } ?: ""
                            val isNextDateBreak = nextDate != currentDate && nextDate.isNotEmpty()
                            val isLastInGroup = nextMessage == null ||
                                nextMessage.isSystemMessage ||
                                nextMessage.sender != message.sender ||
                                isNextDateBreak

                            // Hide sender name inside 1-on-1 DMs; show sender attribution + avatar ONLY in group chats
                            val showSenderName = isGroupChat && !isMe && message.sender.isNotBlank() && isFirstInGroup
                            val showGroupAvatar = isGroupChat && !isMe && message.sender.isNotBlank()

                            ExpressiveChatBubble(
                                message = message,
                                isMe = isMe,
                                showSenderName = showSenderName,
                                showGroupAvatar = showGroupAvatar,
                                isFirstInGroup = isFirstInGroup,
                                isLastInGroup = isLastInGroup,
                                mediaDir = chatMediaDir,
                                audioPlayerManager = viewModel.audioPlayerManager,
                                onImageClick = onImageClick,
                                onVideoClick = onVideoClick,
                                onConfirmation = onConfirmation
                            )
                        }
                    }
                }

                // WhatsApp-style Quick Scroll-to-Bottom Button
                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = androidx.compose.animation.scaleIn(initialScale = 0.78f) + fadeIn(tween(150)),
                    exit = androidx.compose.animation.scaleOut(targetScale = 0.78f) + fadeOut(tween(130)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .wasmElasticClickable(shape = CircleShape) {
                                scope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.lastIndex)
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Go to latest message",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Centered pill header indicating date transitions.
 */
@Composable
fun DateSeparatorHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
            tonalElevation = 1.dp
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * WhatsApp-style System Notification Message (e.g. End-to-end encryption, group events).
 */
@Composable
fun SystemMessageChip(content: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Material 3 Expressive Chat Bubble for WhatsApp messages.
 * Groups consecutive messages from the same sender with compact spacing and dynamic corners,
 * and displays the sender name only on the first message of a turn (or after a day break).
 */
@Composable
fun ExpressiveChatBubble(
    message: MessageEntity,
    isMe: Boolean,
    showSenderName: Boolean = false,
    showGroupAvatar: Boolean = false,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    mediaDir: File,
    audioPlayerManager: AudioPlayerManager? = null,
    onImageClick: (File) -> Unit,
    onVideoClick: (File) -> Unit = {},
    onConfirmation: (String) -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current

    // Authentic WhatsApp speech bubble colors
    val bubbleColor = if (isMe) {
        if (isDark) Color(0xFF005C4B) else Color(0xFFD9FDD3)
    } else {
        if (isDark) Color(0xFF202C33) else Color(0xFFFFFFFF)
    }

    val contentTextColor = if (isDark) {
        Color(0xFFE9EDEF)
    } else {
        Color(0xFF111B21)
    }

    val timestampColor = if (isDark) {
        Color(0xFF8696A0)
    } else {
        Color(0xFF667781)
    }

    // Material 3 Expressive corner grouping for individual bubbles
    val bubbleShape = if (isMe) {
        when {
            isFirstInGroup && isLastInGroup -> RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
            isFirstInGroup -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 6.dp)
            isLastInGroup -> RoundedCornerShape(topStart = 16.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            else -> RoundedCornerShape(topStart = 16.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 6.dp)
        }
    } else {
        when {
            isFirstInGroup && isLastInGroup -> RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            isFirstInGroup -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 16.dp)
            isLastInGroup -> RoundedCornerShape(topStart = 6.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
            else -> RoundedCornerShape(topStart = 6.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 16.dp)
        }
    }

    val bottomSpacing = if (isLastInGroup) 6.dp else 2.dp
    val mediaFile = remember(mediaDir, message.mediaName) {
        if (!message.mediaName.isNullOrBlank()) File(mediaDir, message.mediaName) else null
    }
    val isStandaloneSticker = mediaFile != null &&
        message.mediaName!!.endsWith(".webp", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(bottom = bottomSpacing),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (showGroupAvatar) {
            if (isFirstInGroup) {
                ConversationAvatar(
                    title = message.sender,
                    avatarPath = null,
                    fallbackColor = getWhatsAppAvatarColor(message.sender),
                    modifier = Modifier
                        .padding(end = 6.dp, top = 2.dp)
                        .size(28.dp),
                    textStyle = MaterialTheme.typography.labelSmall
                )
            } else {
                Spacer(modifier = Modifier.width(34.dp))
            }
        }

        if (isStandaloneSticker && mediaFile != null && mediaFile.exists()) {
            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                if (showSenderName) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = getWhatsAppAvatarColor(message.sender),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                WasmInlineStickerOrGif(
                    file = mediaFile,
                    isSticker = true,
                    contentColor = contentTextColor,
                    onConfirmation = onConfirmation
                )
                val timeOnly = message.timestamp.substringAfter(',').trim()
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = timeOnly,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = timestampColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        } else {
            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                shadowElevation = 1.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.widthIn(min = 72.dp, max = 310.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
                    if (showSenderName) {
                        Text(
                            text = message.sender,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = getWhatsAppAvatarColor(message.sender),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Render media attachment if present
                    if (mediaFile != null && !message.mediaName.isNullOrBlank()) {
                        MediaAttachmentCard(
                            file = mediaFile,
                            fileName = message.mediaName,
                            bubbleContentColor = contentTextColor,
                            audioPlayerManager = audioPlayerManager,
                            onImageClick = onImageClick,
                            onVideoClick = onVideoClick,
                            onConfirmation = onConfirmation
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    // Render text if not just a file placeholder
                    val isRedundantPlaceholder = !message.mediaName.isNullOrBlank() &&
                            (message.content.equals("${message.mediaName} (file attached)", ignoreCase = true) ||
                             message.content.equals("<attached: ${message.mediaName}>", ignoreCase = true))

                    val isEditedMessage = message.content.endsWith("<This message was edited>", ignoreCase = true)
                    val displayContent = if (isEditedMessage) {
                        message.content.removeSuffix("<This message was edited>").trimEnd()
                    } else {
                        message.content
                    }

                    if (!isRedundantPlaceholder && displayContent.isNotBlank()) {
                        WhatsAppFormattedMessage(
                            content = displayContent,
                            textColor = contentTextColor,
                            onConfirmation = onConfirmation
                        )
                    } else if (message.mediaName.isNullOrBlank() && displayContent.isBlank()) {
                        Text(
                            text = "Unsupported or empty message",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = timestampColor
                        )
                    }

                    // Timestamp and read receipt
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isEditedMessage) {
                            Text(
                                text = "Edited",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontStyle = FontStyle.Italic,
                                color = timestampColor
                            )
                        }
                        val timeOnly = message.timestamp.substringAfter(',').trim()
                        Text(
                            text = timeOnly,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = timestampColor
                        )
                        if (isMe) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = Color(0xFF53BDEB),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders WhatsApp-style rich formatted messages:
 * - Bold: *text*
 * - Italic: _text_
 * - Strikethrough: ~text~
 * - Monospace: ```text```
 * - Inline code: `text`
 * - Grouped / nested combinations: e.g. *_bold and italic_*
 * - Quote block: > quote text (renders with vertical accent stripe)
 * - Bulleted list: * item or - item (renders with bullet points)
 * - Numbered list: 1. item (renders with aligned numbers)
 */
@Composable
fun WhatsAppFormattedMessage(
    content: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onConfirmation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val monoFontFamily = LocalWasmMonoFontFamily.current
    val lines = remember(content) { content.lines() }

    val hasBlockElements = remember(lines) {
        lines.any { line ->
            line.startsWith("> ") ||
            line.startsWith("* ") ||
            line.startsWith("- ") ||
            line.matches(Regex("""^\d+\.\s.*"""))
        }
    }

    if (!hasBlockElements) {
        val annotated = remember(content, isDark, textColor, monoFontFamily) {
            parseWhatsAppInlineFormattedText(content, isDark, textColor, monoFontFamily)
        }
        WasmClickableAnnotatedText(
            text = annotated,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            onConfirmation = onConfirmation,
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            lines.forEach { rawLine ->
                when {
                    rawLine.startsWith("> ") -> {
                        val quoteText = rawLine.removePrefix("> ")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .padding(vertical = 2.dp)
                                    .height(18.dp)
                                    .background(
                                        color = WhatsAppAccentGreen,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            WasmClickableAnnotatedText(
                                text = parseWhatsAppInlineFormattedText(quoteText, isDark, textColor, monoFontFamily),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = textColor.copy(alpha = 0.88f),
                                onConfirmation = onConfirmation
                            )
                        }
                    }
                    rawLine.startsWith("* ") || rawLine.startsWith("- ") -> {
                        val itemText = rawLine.substring(2)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppAccentGreen
                            )
                            WasmClickableAnnotatedText(
                                text = parseWhatsAppInlineFormattedText(itemText, isDark, textColor, monoFontFamily),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                onConfirmation = onConfirmation
                            )
                        }
                    }
                    rawLine.matches(Regex("""^\d+\.\s.*""")) -> {
                        val numPrefix = rawLine.substringBefore(". ") + ". "
                        val itemText = rawLine.substringAfter(". ")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = numPrefix,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = WhatsAppAccentGreen
                            )
                            WasmClickableAnnotatedText(
                                text = parseWhatsAppInlineFormattedText(itemText, isDark, textColor, monoFontFamily),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                onConfirmation = onConfirmation
                            )
                        }
                    }
                    else -> {
                        WasmClickableAnnotatedText(
                            text = parseWhatsAppInlineFormattedText(rawLine, isDark, textColor, monoFontFamily),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            onConfirmation = onConfirmation
                        )
                    }
                }
            }
        }
    }
}

/**
 * Parses single-line inline formatting tags (`*bold*`, `_italic_`, `~strike~`, `` `code` ``, ```` ```mono``` ````).
 * Never matches across line breaks (`\n`).
 */
fun parseWhatsAppInlineFormattedText(
    text: String,
    isDark: Boolean,
    textColor: Color = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21),
    monoFontFamily: FontFamily = GoogleSansCodeFontFamily
): AnnotatedString = ChatRichTextParser.parseInlineSingleLine(
    text = text,
    isDark = isDark,
    textColor = textColor,
    monoFontFamily = monoFontFamily
)

/**
 * Attachment card rendering photos, audio, documents with memory caching.
 */
@Composable
fun MediaAttachmentCard(
    file: File,
    fileName: String,
    bubbleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    audioPlayerManager: AudioPlayerManager? = null,
    onImageClick: (File) -> Unit,
    onVideoClick: (File) -> Unit = {},
    onConfirmation: (String) -> Unit = {}
) {
    val isPhoto = fileName.endsWith(".jpg", true) ||
        fileName.endsWith(".jpeg", true) ||
        fileName.endsWith(".png", true)
    val isSticker = fileName.endsWith(".webp", true)
    val isGif = fileName.endsWith(".gif", true)
    val isAudio = fileName.endsWith(".mp3", true) ||
        fileName.endsWith(".opus", true) ||
        fileName.endsWith(".ogg", true) ||
        fileName.endsWith(".m4a", true) ||
        fileName.endsWith(".aac", true) ||
        fileName.endsWith(".wav", true)
    val isVideo = fileName.endsWith(".mp4", true) ||
        fileName.endsWith(".mkv", true) ||
        fileName.endsWith(".3gp", true) ||
        fileName.endsWith(".webm", true)

    when {
        isSticker && file.exists() -> {
            WasmInlineStickerOrGif(
                file = file,
                isSticker = true,
                contentColor = bubbleContentColor,
                onConfirmation = onConfirmation
            )
        }
        isGif && file.exists() -> {
            WasmInlineStickerOrGif(
                file = file,
                isSticker = false,
                contentColor = bubbleContentColor,
                onConfirmation = onConfirmation
            )
        }
        isPhoto && file.exists() -> {
            val cached = BitmapMemoryCache.get(file.absolutePath)
            val bitmapState = produceState(initialValue = cached, key1 = file.absolutePath) {
                if (value == null) {
                    val decoded = withContext(Dispatchers.IO) {
                        decodeSampledBitmap(file, reqWidth = 600, reqHeight = 600)
                    }
                    decoded?.let {
                        BitmapMemoryCache.put(file.absolutePath, it)
                        value = it
                    }
                }
            }

            bitmapState.value?.let { imgBitmap ->
                Image(
                    bitmap = imgBitmap,
                    contentDescription = fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onImageClick(file) }
                )
            }
        }
        isVideo && file.exists() -> {
            WasmInlineVideoCard(
                file = file,
                onClick = { onVideoClick(file) }
            )
        }
        isAudio && file.exists() && audioPlayerManager != null -> {
            WasmInlineAudioBubble(
                file = file,
                audioPlayerManager = audioPlayerManager,
                accentColor = WhatsAppAccentGreen,
                contentColor = bubbleContentColor,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                onConfirmation = onConfirmation
            )
        }
        else -> {
            val sizeText = if (file.exists()) formatFileSize(file.length()) else ""
            WasmExternalFileCard(
                file = file,
                fileName = fileName,
                sizeText = sizeText,
                contentColor = bubbleContentColor,
                onConfirmation = onConfirmation
            )
        }
    }
}

/**
 * Fullscreen image preview modal dialog.
 */
@Composable
fun FullScreenImageDialog(file: File, onDismiss: () -> Unit) {
    val dialogScale = remember { Animatable(0.90f) }
    val dialogAlpha = remember { Animatable(0f) }
    LaunchedEffect(file.absolutePath) {
        dialogScale.animateTo(1f, spring(stiffness = 420f, dampingRatio = 0.76f))
    }
    LaunchedEffect(file.absolutePath) {
        dialogAlpha.animateTo(1f, tween(200))
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f * dialogAlpha.value))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val bitmap = remember(file.absolutePath) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            }
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .graphicsLayer {
                            scaleX = dialogScale.value
                            scaleY = dialogScale.value
                            alpha = dialogAlpha.value
                        }
                )
            }
        }
    }
}

/**
 * Identity Selection Dialog for choosing who "Me" is.
 */
@Composable
fun IdentitySelectionDialog(
    senders: List<String>,
    currentSelected: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Person, contentDescription = null) },
        title = { Text("Who are you in this chat?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select your name so we can align your messages to the right side:",
                    style = MaterialTheme.typography.bodySmall
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(senders) { name ->
                        val isSelected = name == currentSelected
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelected(name) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = getWhatsAppAvatarColor(name),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Material 3 Expressive Empty State view for WhatsApp.
 */
@Composable
fun ExpressiveEmptyState(
    onImportClick: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = WhatsAppAccentGreen.copy(alpha = 0.16f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_whatsapp),
                        contentDescription = "WhatsApp",
                        tint = WhatsAppAccentGreen,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Text(
                text = "No WhatsApp Chats Saved",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Import any WhatsApp export file (.txt or .zip with attachments). Every individual chat is saved separately so you can browse all your conversations offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("💡 How to export from WhatsApp:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("1. In WhatsApp, open any chat > tap ⋮ > More > Export chat", style = MaterialTheme.typography.labelSmall)
                    Text("2. Select 'Attach Media' (.zip) or 'Without Media' (.txt)", style = MaterialTheme.typography.labelSmall)
                    Text("3. Tap 'Import Chat File' below to save it into Wasm", style = MaterialTheme.typography.labelSmall)
                }
            }

            Button(
                onClick = onImportClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import Chat File", fontWeight = FontWeight.SemiBold)
            }

            if (onBack != null) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back to Platform Selection")
                }
            }
        }
    }
}
