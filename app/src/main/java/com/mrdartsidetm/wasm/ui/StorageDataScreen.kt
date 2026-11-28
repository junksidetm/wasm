package com.mrdartsidetm.wasm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrdartsidetm.wasm.ui.instagram.InstagramViewModel
import com.mrdartsidetm.wasm.ui.instagram.getAvatarColor
import com.mrdartsidetm.wasm.ui.motion.WasmMechanicalText
import com.mrdartsidetm.wasm.ui.motion.WasmMotionTokens
import com.mrdartsidetm.wasm.ui.motion.wasmFeatheredVerticalViewport
import com.mrdartsidetm.wasm.ui.motion.wasmLuminousElasticClickable
import com.mrdartsidetm.wasm.util.MediaCategoryBreakdown
import com.mrdartsidetm.wasm.util.StorageMediaCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ChatStorageRowItem(
    val id: String,
    val title: String,
    val avatarPath: String?,
    val messageCount: Int,
    val breakdown: MediaCategoryBreakdown,
    val fallbackColor: Color
)

/**
 * Dedicated Storage & Data Management Page.
 * Includes the Whatsapp | Instagram connected button toggle, platform-wide wipe actions,
 * bulk media-type deletion (Images, Videos, Voice Notes, GIFs, Stickers),
 * and per-chat individual storage inspection & deletion.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun StorageDataScreen(
    whatsappViewModel: ChatViewModel,
    instagramViewModel: InstagramViewModel,
    initialPlatform: String = "whatsapp",
    onConfirmation: (String) -> Unit = {},
    onBack: () -> Unit
) {
    var activePlatform by remember { mutableStateOf(initialPlatform) }
    BackHandler(onBack = onBack)
    var refreshNonce by remember { mutableIntStateOf(0) }

    val waConversations by whatsappViewModel.conversations.collectAsStateWithLifecycle()
    val igConversations by instagramViewModel.conversations.collectAsStateWithLifecycle()

    var selectedRowForManage by remember { mutableStateOf<ChatStorageRowItem?>(null) }
    var confirmBulkAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val rows by produceState<List<ChatStorageRowItem>>(
        initialValue = emptyList(),
        key1 = activePlatform,
        key2 = waConversations,
        key3 = igConversations to refreshNonce
    ) {
        value = withContext(Dispatchers.IO) {
            if (activePlatform == "whatsapp") {
                waConversations.map { conv ->
                    ChatStorageRowItem(
                        id = conv.id,
                        title = conv.title,
                        avatarPath = conv.avatarPath,
                        messageCount = conv.messageCount,
                        breakdown = whatsappViewModel.computeConversationBreakdown(conv),
                        fallbackColor = getWhatsAppAvatarColor(conv.title)
                    )
                }.sortedByDescending { it.breakdown.totalBytes }
            } else {
                igConversations.map { conv ->
                    ChatStorageRowItem(
                        id = conv.id,
                        title = conv.title,
                        avatarPath = conv.avatarPath,
                        messageCount = conv.messageCount,
                        breakdown = instagramViewModel.computeConversationBreakdown(conv),
                        fallbackColor = getAvatarColor(conv.title)
                    )
                }.sortedByDescending { it.breakdown.totalBytes }
            }
        }
    }

    val totalPlatformBytes = remember(rows) { rows.sumOf { it.breakdown.totalBytes } }

    confirmBulkAction?.let { (message, action) ->
        AlertDialog(
            onDismissRequest = { confirmBulkAction = null },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Deletion") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        action()
                        refreshNonce++
                        confirmBulkAction = null
                        onConfirmation("Storage updated")
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBulkAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedRowForManage?.let { row ->
        AlertDialog(
            onDismissRequest = { selectedRowForManage = null },
            title = {
                Text(
                    text = row.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Total estimated size: ${formatFileSize(row.breakdown.totalBytes)} (${row.messageCount} messages)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val deletableCategories = listOf(
                        StorageMediaCategory.IMAGES,
                        StorageMediaCategory.VIDEOS,
                        StorageMediaCategory.VOICE_NOTES,
                        StorageMediaCategory.GIFS,
                        StorageMediaCategory.STICKERS
                    )
                    deletableCategories.forEach { category ->
                        val catBytes = row.breakdown.bytesFor(category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = catBytes > 0L) {
                                    if (activePlatform == "whatsapp") {
                                        whatsappViewModel.deleteConversationCategory(row.id, category)
                                    } else {
                                        instagramViewModel.deleteConversationCategory(row.id, category)
                                    }
                                    refreshNonce++
                                    selectedRowForManage = null
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Delete ${category.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (catBytes > 0L) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = formatFileSize(catBytes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (catBytes > 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (activePlatform == "whatsapp") {
                            whatsappViewModel.deleteConversation(row.id)
                        } else {
                            instagramViewModel.deleteConversation(row.id)
                        }
                        refreshNonce++
                        selectedRowForManage = null
                        onConfirmation("Chat removed from storage")
                    }
                ) {
                    Text("Delete Entire Chat", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRowForManage = null }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Storage & Data",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .wasmFeatheredVerticalViewport(topFadeFraction = 0.02f, bottomFadeFraction = 0.05f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Connected Whatsapp | Instagram platform selector
            item {
                HomeConnectedPlatformToggle(
                    activePlatform = activePlatform,
                    onSelectPlatform = { activePlatform = it }
                )
            }

            // Platform-level Actions & Media-Type Purge Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (activePlatform == "whatsapp") "WhatsApp Storage" else "Instagram Storage",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                WasmMechanicalText(
                                    text = "${rows.size} chats • ${formatFileSize(totalPlatformBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    val label = if (activePlatform == "whatsapp") "WhatsApp" else "Instagram"
                                    confirmBulkAction = "Delete ALL $label conversations, messages, and extracted media?" to {
                                        if (activePlatform == "whatsapp") {
                                            whatsappViewModel.clearAllWhatsApp()
                                        } else {
                                            instagramViewModel.clearInstagramData()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Delete All")
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text(
                            text = "Delete by Media Category",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                StorageMediaCategory.IMAGES,
                                StorageMediaCategory.VIDEOS,
                                StorageMediaCategory.VOICE_NOTES,
                                StorageMediaCategory.GIFS,
                                StorageMediaCategory.STICKERS
                            ).forEach { category ->
                                val catTotal = rows.sumOf { it.breakdown.bytesFor(category) }
                                OutlinedButton(
                                    enabled = catTotal > 0L,
                                    onClick = {
                                        val platformLabel = if (activePlatform == "whatsapp") "WhatsApp" else "Instagram"
                                        confirmBulkAction = "Delete all ${category.label} from $platformLabel (${formatFileSize(catTotal)})?" to {
                                            if (activePlatform == "whatsapp") {
                                                whatsappViewModel.deleteAllCategoryMedia(category)
                                            } else {
                                                instagramViewModel.deleteAllCategoryMedia(category)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${category.label} (${formatFileSize(catTotal)})",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Individual Chats Heading
            item {
                Text(
                    text = "Individual Chats",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (rows.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No chats stored for this platform.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(rows, key = { it.id }) { row ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement(WasmMotionTokens.SpringListPlacement)
                            .wasmLuminousElasticClickable(
                                shape = RoundedCornerShape(18.dp),
                                maxStretchPercent = 0.025f,
                                pressedScale = 0.975f,
                                onClick = { selectedRowForManage = row }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ConversationAvatar(
                                title = row.title,
                                avatarPath = row.avatarPath,
                                fallbackColor = row.fallbackColor,
                                modifier = Modifier.size(44.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${row.messageCount} msgs • Img ${formatFileSize(row.breakdown.imageBytes)} • Vid ${formatFileSize(row.breakdown.videoBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = formatFileSize(row.breakdown.totalBytes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = { selectedRowForManage = row },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Manage chat storage",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
