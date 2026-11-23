package com.example.spatialboard.ui

import com.example.spatialboard.ble.DetectedBeacon
import com.example.spatialboard.data.DataSourceMode
import com.example.spatialboard.data.SpatialSettingsStore
import com.example.spatialboard.data.remote.ServerSyncState
import com.example.spatialboard.model.BeaconItem
import com.example.spatialboard.model.BoardItem

data class ServerConfig(
    val host: String = SpatialSettingsStore.DEFAULT_SERVER_HOST,
    val port: Int = SpatialSettingsStore.DEFAULT_SERVER_PORT,
    val mode: DataSourceMode = DataSourceMode.LOCAL,
    val syncState: ServerSyncState = ServerSyncState.Idle
) {
    val baseUrl: String get() = "http://$host:$port"
}

data class SpatialBoardUiState(
    val serverConfig: ServerConfig = ServerConfig(),
    val minRssiThreshold: Int = SpatialSettingsStore.DEFAULT_MIN_RSSI,
    val beacons: List<BeaconItem> = emptyList(),
    val activeBeacons: List<DetectedBeacon> = emptyList(),
    val availableArBoards: List<BoardItem> = emptyList(),
    val pendingArBoard: BoardItem? = null
) {
    val dataSourceMode: DataSourceMode get() = serverConfig.mode
    val serverHost: String get() = serverConfig.host
    val serverPort: Int get() = serverConfig.port
    val serverSyncState: ServerSyncState get() = serverConfig.syncState
}