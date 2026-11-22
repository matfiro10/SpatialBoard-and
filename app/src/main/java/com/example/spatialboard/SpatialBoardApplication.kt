package com.example.spatialboard

import android.app.Application
import com.example.spatialboard.data.RoomSpatialRepository
import com.example.spatialboard.data.SpatialRepository
import com.example.spatialboard.data.local.SpatialDatabase

class SpatialBoardApplication : Application() {
    val database: SpatialDatabase by lazy {
        SpatialDatabase.getInstance(this)
    }

    val repository: SpatialRepository by lazy {
        RoomSpatialRepository(database.boardDao())
    }
}
