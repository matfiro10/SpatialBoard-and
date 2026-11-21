package com.example.spatialboard.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface ActiveOverlay {
    data object None : ActiveOverlay
    data object Permissions : ActiveOverlay
    data class NavigationCard(@StringRes val titleResId: Int) : ActiveOverlay
}

@Composable
fun OverlayWindow(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 420.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = maxWidth),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        content = content
    )
}
