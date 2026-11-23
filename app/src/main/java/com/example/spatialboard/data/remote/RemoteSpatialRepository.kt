package com.example.spatialboard.data.remote

import com.example.spatialboard.ble.DetectedBeacon
import com.example.spatialboard.data.SpatialRepository
import kotlinx.coroutines.flow.StateFlow

interface RemoteSpatialRepository : SpatialRepository {
    val syncState: StateFlow<ServerSyncState>
    suspend fun testConnection(): Result<String>
    suspend fun refreshForBeacons(detectedBeacons: List<DetectedBeacon>)
}
