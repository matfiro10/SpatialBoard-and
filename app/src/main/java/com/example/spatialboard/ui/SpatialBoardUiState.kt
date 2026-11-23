package com.example.spatialboard.ui

import com.example.spatialboard.ble.DetectedBeacon
import com.example.spatialboard.data.DataSourceMode
import com.example.spatialboard.data.SpatialSettingsStore
import com.example.spatialboard.data.remote.ServerSyncState
import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem

data class SpatialBoardUiState(
    val dataSourceMode: DataSourceMode = DataSourceMode.LOCAL,
    val serverHost: String = SpatialSettingsStore.DEFAULT_SERVER_HOST,
    val serverPort: Int = SpatialSettingsStore.DEFAULT_SERVER_PORT,
    val minRssiThreshold: Int = SpatialSettingsStore.DEFAULT_MIN_RSSI,
    val serverSyncState: ServerSyncState = ServerSyncState.Idle,
    val beacons: List<BeaconItem> = emptyList(),
    val activeBeacons: List<DetectedBeacon> = emptyList(),
    val availableArBoards: List<BoardItem> = emptyList(),
    val pendingArBoard: BoardItem? = null
)
