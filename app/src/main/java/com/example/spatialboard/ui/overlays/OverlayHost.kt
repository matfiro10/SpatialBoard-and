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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onNavigate: (ActiveOverlay) -> Unit,
    onBack: () -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var boards by remember { mutableStateOf(SampleSpatialData.initialBoards) }
    val beacons by remember { mutableStateOf(SampleSpatialData.initialBeacons) }

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
                                boards = boards,
                                onBoardClick = { board ->
                                    onNavigate(ActiveOverlay.BoardDetails(board.id))
                                }
                            )
                        }
                        R.string.nav_beacons -> {
                            BeaconsOverlayContent(
                                beacons = beacons,
                                onBeaconClick = { beacon ->
                                    onNavigate(ActiveOverlay.BeaconDetails(beacon.id))
                                }
                            )
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
            is ActiveOverlay.BoardDetails -> {
                val board = boards.find { it.id == overlay.boardId }
                OverlayWindow(
                    title = board?.title ?: stringResource(R.string.nav_boards),
                    onBack = onBack,
                    onClose = onCloseAll
                ) {
                    if (board != null) {
                        BoardDetailsOverlayContent(
                            board = board,
                            onSave = { updatedBoard ->
                                boards = boards.map { item ->
                                    if (item.id == updatedBoard.id) updatedBoard else item
                                }
                            }
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.item_not_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
            is ActiveOverlay.BeaconDetails -> {
                val beacon = beacons.find { it.id == overlay.beaconId }
                OverlayWindow(
                    title = beacon?.name ?: stringResource(R.string.nav_beacons),
                    onBack = onBack,
                    onClose = onCloseAll
                ) {
                    if (beacon != null) {
                        BeaconDetailsOverlayContent(beacon = beacon)
                    } else {
                        Text(
                            text = stringResource(R.string.item_not_found),
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
