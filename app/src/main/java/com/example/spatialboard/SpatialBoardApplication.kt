package com.example.spatialboard

import android.app.Application
import com.example.spatialboard.data.RoomSpatialRepository
import com.example.spatialboard.data.SpatialRepository
import com.example.spatialboard.data.SpatialSettingsStore
import com.example.spatialboard.data.local.SpatialDatabase
import com.example.spatialboard.data.remote.NetworkSpatialRepository
import com.example.spatialboard.data.remote.SpatialBoardApiClient

class SpatialBoardApplication : Application() {
    val database: SpatialDatabase by lazy {
        SpatialDatabase.getInstance(this)
    }

    val settingsStore: SpatialSettingsStore by lazy {
        SpatialSettingsStore(this)
    }

    val repository: SpatialRepository by lazy {
        RoomSpatialRepository(database.boardDao())
    }

    val apiClient: SpatialBoardApiClient by lazy {
        SpatialBoardApiClient(settingsStore)
    }

    val networkRepository: NetworkSpatialRepository by lazy {
        NetworkSpatialRepository(this, apiClient)
    }
}
