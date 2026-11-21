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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spatialboard.R
import com.example.spatialboard.permissions.PermissionWindow
import com.example.spatialboard.ui.SpatialBoardViewModel

@Composable
fun OverlayHost(
    activeOverlay: ActiveOverlay,
    isPermanentlyDenied: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigate: (ActiveOverlay) -> Unit,
    onBack: () -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier,
    spatialViewModel: SpatialBoardViewModel = viewModel()
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
                            val boards by spatialViewModel.boards.collectAsStateWithLifecycle()
                            BoardsOverlayContent(
                                boards = boards,
                                onBoardClick = { board ->
                                    onNavigate(ActiveOverlay.BoardDetails(board.id))
                                }
                            )
                        }
                        R.string.nav_beacons -> {
                            val beacons by spatialViewModel.beacons.collectAsStateWithLifecycle()
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
                val boards by spatialViewModel.boards.collectAsStateWithLifecycle()
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
                                spatialViewModel.saveBoard(updatedBoard)
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
                val beacons by spatialViewModel.beacons.collectAsStateWithLifecycle()
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
