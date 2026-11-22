package com.example.spatialboard.ui.overlays

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.spatialboard.R
import com.example.spatialboard.permissions.PermissionWindow
import com.example.spatialboard.ui.BoardDetailState
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
    spatialViewModel: SpatialBoardViewModel = viewModel(factory = SpatialBoardViewModel.Factory)
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
                            val boards = spatialViewModel.boardsPagingFlow.collectAsLazyPagingItems()
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
                        R.string.nav_add -> {
                            val availableBeacons by spatialViewModel.beacons.collectAsStateWithLifecycle()
                            CreateBoardOverlayContent(
                                availableBeacons = availableBeacons,
                                preselectedBeaconId = null,
                                onCreateBoard = { newBoard ->
                                    spatialViewModel.saveBoard(newBoard)
                                    onNavigate(ActiveOverlay.NavigationCard(R.string.nav_boards))
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
                val detailState by remember(overlay.boardId) {
                    spatialViewModel.observeBoardDetail(overlay.boardId)
                }.collectAsStateWithLifecycle(initialValue = BoardDetailState.Loading)

                val windowTitle = (detailState as? BoardDetailState.Ready)?.board?.title
                    ?: stringResource(R.string.nav_boards)

                OverlayWindow(
                    title = windowTitle,
                    onBack = onBack,
                    onClose = onCloseAll
                ) {
                    when (val state = detailState) {
                        BoardDetailState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                        is BoardDetailState.Ready -> {
                            BoardDetailsOverlayContent(
                                board = state.board,
                                onSave = { updatedBoard ->
                                    spatialViewModel.saveBoard(updatedBoard)
                                }
                            )
                        }
                        BoardDetailState.NotFound -> {
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
            is ActiveOverlay.BeaconDetails -> {
                val initialBeacon = remember(overlay.beaconId) {
                    spatialViewModel.beacons.value.find { it.id == overlay.beaconId }
                }
                val liveBeacon by remember(overlay.beaconId) {
                    spatialViewModel.observeBeacon(overlay.beaconId)
                }.collectAsStateWithLifecycle(initialValue = initialBeacon)

                var lastKnownBeacon by remember(overlay.beaconId) {
                    mutableStateOf(initialBeacon)
                }

                LaunchedEffect(liveBeacon) {
                    if (liveBeacon != null) {
                        lastKnownBeacon = liveBeacon
                    }
                }

                val displayedBeacon = liveBeacon ?: lastKnownBeacon?.copy(isInRange = false)

                OverlayWindow(
                    title = displayedBeacon?.name ?: stringResource(R.string.nav_beacons),
                    onBack = onBack,
                    onClose = onCloseAll
                ) {
                    if (displayedBeacon != null) {
                        BeaconDetailsOverlayContent(
                            beacon = displayedBeacon,
                            onCreateBoard = { beaconId ->
                                onNavigate(ActiveOverlay.CreateBoard(preselectedBeaconId = beaconId))
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
            is ActiveOverlay.CreateBoard -> {
                val availableBeacons by spatialViewModel.beacons.collectAsStateWithLifecycle()

                OverlayWindow(
                    title = stringResource(R.string.create_board_window_title),
                    onBack = onBack,
                    onClose = onCloseAll
                ) {
                    CreateBoardOverlayContent(
                        availableBeacons = availableBeacons,
                        preselectedBeaconId = overlay.preselectedBeaconId,
                        onCreateBoard = { newBoard ->
                            spatialViewModel.saveBoard(newBoard)
                            onBack()
                        }
                    )
                }
            }
        }
    }
}
