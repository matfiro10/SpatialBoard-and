package com.example.spatialboard.ui.overlays

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.spatialboard.data.DataSourceMode
import com.example.spatialboard.permissions.PermissionWindow
import com.example.spatialboard.ui.BoardDetailState
import com.example.spatialboard.ui.SpatialBoardUiState
import com.example.spatialboard.ui.SpatialBoardViewModel
import com.example.spatialboard.ui.components.ActiveOverlay
import com.example.spatialboard.ui.components.OverlayWindow
import com.example.spatialboard.ui.components.rememberOverlayContentMaxHeight
import com.example.spatialboard.ui.settings.BleRangeSettingsSection
import com.example.spatialboard.ui.settings.ServerSettingsSection

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
    val uiState by spatialViewModel.uiState.collectAsStateWithLifecycle()

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
                            val boards =
                                spatialViewModel.boardsPagingFlow.collectAsLazyPagingItems()
                            BoardsOverlayContent(
                                boards = boards,
                                onBoardClick = { board ->
                                    onNavigate(ActiveOverlay.BoardDetails(board.id))
                                }
                            )
                        }

                        R.string.nav_beacons -> {
                            BeaconsOverlayContent(
                                beacons = uiState.beacons,
                                onBeaconClick = { beacon ->
                                    onNavigate(ActiveOverlay.BeaconDetails(beacon.id))
                                }
                            )
                        }

                        R.string.nav_add -> {
                            CreateBoardOverlayContent(
                                availableBeacons = uiState.beacons,
                                preselectedBeaconId = null,
                                onCreateBoard = { newBoard ->
                                    spatialViewModel.saveBoard(newBoard)
                                    onNavigate(ActiveOverlay.NavigationCard(R.string.nav_boards))
                                }
                            )
                        }

                        else -> {
                            SettingsOverlayContent(
                                uiState = uiState,
                                onModeChange = spatialViewModel::setDataSourceMode,
                                onSaveEndpoint = spatialViewModel::updateServerEndpoint,
                                onTestConnection = spatialViewModel::testServerConnection,
                                onThresholdChange = spatialViewModel::setMinRssiThreshold
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
                                nearbyBeacons = uiState.activeBeacons,
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
                    uiState.beacons.find { it.id == overlay.beaconId }
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
                OverlayWindow(
                    title = stringResource(R.string.create_board_window_title),
                    onBack = onBack,
                    onClose = onCloseAll
                ) {
                    CreateBoardOverlayContent(
                        availableBeacons = uiState.beacons,
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

@Composable
private fun SettingsOverlayContent(
    uiState: SpatialBoardUiState,
    onModeChange: (DataSourceMode) -> Unit,
    onSaveEndpoint: (String, Int) -> Unit,
    onTestConnection: () -> Unit,
    onThresholdChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxContentHeight = rememberOverlayContentMaxHeight()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxContentHeight),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "settings_server") {
            ServerSettingsSection(
                dataSourceMode = uiState.dataSourceMode,
                serverHost = uiState.serverHost,
                serverPort = uiState.serverPort,
                syncState = uiState.serverSyncState,
                onModeChange = onModeChange,
                onSaveEndpoint = onSaveEndpoint,
                onTestConnection = onTestConnection
            )
        }
        item(key = "settings_ble_range") {
            BleRangeSettingsSection(
                minRssiThreshold = uiState.minRssiThreshold,
                onThresholdChange = onThresholdChange
            )
        }
    }
}
