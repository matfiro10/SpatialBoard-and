package com.example.spatialboard.ui.overlays

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R
import com.example.spatialboard.permissions.PermissionWindow

@Composable
fun OverlayHost(
    activeOverlay: ActiveOverlay,
    isPermanentlyDenied: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = activeOverlay,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.94f))
                .togetherWith(fadeOut() + scaleOut(targetScale = 0.94f))
        },
        contentAlignment = Alignment.Center,
        modifier = modifier,
        label = "overlayContent"
    ) { overlay ->
        when (overlay) {
            ActiveOverlay.None -> Unit
            ActiveOverlay.Permissions -> {
                PermissionWindow(
                    isPermanentlyDenied = isPermanentlyDenied,
                    onRequestPermissions = onRequestPermissions,
                    onOpenSettings = onOpenSettings
                )
            }
            is ActiveOverlay.NavigationCard -> {
                OverlayWindow(
                    title = stringResource(overlay.titleResId),
                    onClose = onCloseAll
                ) {
                    when (overlay.titleResId) {
                        R.string.nav_boards -> {
                            BoardsOverlayContent(
                                onBoardClick = { onCloseAll() }
                            )
                        }
                        R.string.nav_beacons -> {
                            BeaconsOverlayContent()
                        }
                        else -> {
                            Text(
                                text = stringResource(R.string.overlay_coming_soon),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
