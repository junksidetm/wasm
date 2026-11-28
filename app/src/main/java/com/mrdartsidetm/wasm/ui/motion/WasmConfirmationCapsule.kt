package com.mrdartsidetm.wasm.ui.motion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Cancels any pending confirmation banner and immediately presents the latest message
 * cleanly, modeled on Expressive Clock's `AlarmConfirmationPresenter`.
 */
class WasmConfirmationPresenter(private val scope: CoroutineScope) {
    val hostState = SnackbarHostState()
    private var request: Job? = null

    fun show(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        request?.cancel()
        request = scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(message = message, duration = duration)
        }
    }
}

/**
 * Compact intrinsic-width capsule confirmation badge modeled on Expressive Clock's `AlarmConfirmation`:
 * zero drop shadow, zero tonal elevation bloat, accessible line-wrapping at large font scales.
 */
@Composable
fun WasmConfirmationCapsule(
    data: SnackbarData,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.CheckCircle
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp * fontScale),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
