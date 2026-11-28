package com.mrdartsidetm.wasm.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrdartsidetm.wasm.BuildConfig
import com.mrdartsidetm.wasm.R
import com.mrdartsidetm.wasm.ui.localization.LocalWasmStrings
import com.mrdartsidetm.wasm.ui.theme.GoogleSansCodeFontFamily
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive About Screen for Wasm, modeled after LongShotPDF's Universal About Page:
 * - CenterAlignedTopAppBar with back navigation
 * - Hero visualizer: 200dp squircle + full-bleed 150dp app logo (`WASM - Logo.svg`, 75% fill)
 *   flanked by ambient animated M3 Expressive Bun and 9-Side Cookie shapes
 * - Dynamic version badge (`BuildConfig.VERSION_NAME`) with 7-tap Developer Mode Easter Egg
 * - Developer Profile Card (`Abhijeet Yadav`) with GitHub & Email action pills
 * - Community FOSS "June" connected tile group (Updates, GitHub Main, GitLab & Codeberg Mirrors,
 *   Material 3 Guidelines, and in-app Apache 2.0 License bottom sheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalWasmStrings.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(onBack = onBack)

    val appVersion = BuildConfig.VERSION_NAME
    val appBuild = BuildConfig.VERSION_CODE

    var showLicenseSheet by remember { mutableStateOf(false) }
    val licenseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var versionTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    fun onVersionTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 3000L) {
            versionTapCount = 0
        }
        lastTapTime = now
        versionTapCount++

        coroutineScope.launch {
            if (versionTapCount >= 7) {
                versionTapCount = 0
                snackbarHostState.showSnackbar("Developer Mode Activated!")
            } else if (versionTapCount >= 3) {
                val remaining = 7 - versionTapCount
                snackbarHostState.showSnackbar("You are $remaining step${if (remaining > 1) "s" else ""} away from Developer Mode")
            }
        }
    }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Could not open link: $url")
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AboutShapesAmbient")

    val bunRotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BunRotation"
    )
    val bunOffsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BunFloat"
    )

    val cookieRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CookieRotation"
    )
    val cookieScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CookieScale"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = strings.aboutTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                // Hero Illustration: Squircle Container + 75% Full-Bleed Logo + Ambient M3 Shapes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = 18.dp, y = (-32).dp + bunOffsetY.dp)
                            .graphicsLayer {
                                rotationZ = bunRotation
                            }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.shape_bun),
                            contentDescription = "Expressive Bun Shape",
                            modifier = Modifier.size(135.dp),
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.88f)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (-12).dp, y = 48.dp)
                            .graphicsLayer {
                                rotationZ = cookieRotation
                                scaleX = cookieScale
                                scaleY = cookieScale
                            }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.shape_9_side_cookie),
                            contentDescription = "Expressive 9-Side Cookie Shape",
                            modifier = Modifier.size(145.dp),
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.88f)
                            )
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(200.dp)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(30.dp)
                            ),
                        shape = RoundedCornerShape(30.dp),
                        color = Color.Transparent
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Wasm Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // App Title, Summary & Clickable Version Pill
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Wasm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Offline WhatsApp & Instagram chat archive viewer built in pure native Kotlin & Jetpack Compose with Material 3 Expressive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        onClick = { onVersionTap() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Version $appVersion (Build $appBuild)",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = GoogleSansCodeFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Developer Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Abhijeet Yadav", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Developer • junksidetm / mrdarksidetm") },
                            leadingContent = {
                                Image(
                                    painter = painterResource(id = R.drawable.developer),
                                    contentDescription = "Developer Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { openUrl("https://github.com/junksidetm/wasm") }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = { openUrl("https://github.com/junksidetm/wasm") },
                                shape = RoundedCornerShape(100),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_github),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GitHub",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                onClick = { openUrl("mailto:contact.dsidetm@gmail.com") },
                                shape = RoundedCornerShape(100),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_email_at),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Email",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Community FOSS Connected Settings List (June / LongShotPDF pattern)
                JuneAboutGroup(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    JuneAboutTile(
                        title = "Check for Updates",
                        subtitle = "v$appVersion • GitHub Releases",
                        leadingIcon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        index = 0,
                        count = 6,
                        onClick = { openUrl("https://github.com/junksidetm/wasm/releases/latest") }
                    )

                    JuneAboutTile(
                        title = "Open Source Repository",
                        subtitle = "github.com/junksidetm/wasm (Main)",
                        leadingIcon = { Icon(Icons.Rounded.Code, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        index = 1,
                        count = 6,
                        onClick = { openUrl("https://github.com/junksidetm/wasm") }
                    )

                    JuneAboutTile(
                        title = "GitLab Mirror",
                        subtitle = "gitlab.com/mrdarksidetm/wasm",
                        leadingIcon = { Icon(Icons.Rounded.Storage, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        index = 2,
                        count = 6,
                        onClick = { openUrl("https://gitlab.com/mrdarksidetm/wasm") }
                    )

                    JuneAboutTile(
                        title = "Codeberg Mirror",
                        subtitle = "codeberg.org/mrdarksidetm/wasm",
                        leadingIcon = { Icon(Icons.Rounded.Public, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        index = 3,
                        count = 6,
                        onClick = { openUrl("https://codeberg.org/mrdarksidetm/wasm") }
                    )

                    JuneAboutTile(
                        title = "Material 3 Guidelines",
                        subtitle = "Official design system authority (m3.material.io)",
                        leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        index = 4,
                        count = 6,
                        onClick = { openUrl("https://m3.material.io/") }
                    )

                    JuneAboutTile(
                        title = "License",
                        subtitle = "Apache License 2.0 (Open in-app)",
                        leadingIcon = { Icon(Icons.Rounded.Gavel, contentDescription = null) },
                        trailingContent = { Icon(Icons.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        index = 5,
                        count = 6,
                        onClick = { showLicenseSheet = true }
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.madeWithLove,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )

            if (showLicenseSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showLicenseSheet = false },
                    sheetState = licenseSheetState,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "License",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Apache License, Version 2.0",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(onClick = {
                                coroutineScope.launch {
                                    licenseSheetState.hide()
                                    showLicenseSheet = false
                                }
                            }) {
                                Text("Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = """
                                      Apache License
                                Version 2.0, January 2004
                             http://www.apache.org/licenses/

        Copyright 2026 Abhijeet Yadav / junksidetm

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
                                    """.trimIndent(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = GoogleSansCodeFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JuneAboutGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun JuneAboutTile(
    title: String,
    subtitle: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    index: Int = 0,
    count: Int = 1,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedCorner by animateDpAsState(
        targetValue = if (isPressed) 28.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "juneAboutItemCornerAnim"
    )

    val itemShape = remember(index, count, animatedCorner) {
        when {
            count <= 1 -> RoundedCornerShape(28.dp)
            index == 0 -> RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomStart = animatedCorner,
                bottomEnd = animatedCorner
            )
            index == count - 1 -> RoundedCornerShape(
                topStart = animatedCorner,
                topEnd = animatedCorner,
                bottomStart = 28.dp,
                bottomEnd = 28.dp
            )
            else -> RoundedCornerShape(animatedCorner)
        }
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = itemShape,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    leadingIcon()
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}
