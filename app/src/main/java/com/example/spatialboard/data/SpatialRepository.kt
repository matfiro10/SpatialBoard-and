package com.example.spatialboard.data

import androidx.paging.PagingData
import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.flow.Flow

interface SpatialRepository {
    fun observeAllBoards(): Flow<List<BoardItem>>
    fun observeBoardsPaged(): Flow<PagingData<BoardItem>>
    fun observeBoardById(boardId: String): Flow<BoardItem?>
    fun observeBeacons(): Flow<List<BeaconItem>>
    fun observeBeaconById(beaconId: String): Flow<BeaconItem?>
    fun observeBoardsForAr(beaconIds: List<String>): Flow<List<BoardItem>>

    suspend fun upsertBoard(board: BoardItem)
    suspend fun upsertBoards(boards: List<BoardItem>)
    suspend fun replaceBoards(boards: List<BoardItem>)
    suspend fun deleteBoard(boardId: String)

    suspend fun upsertBeacons(scannedBeacons: List<BeaconItem>)
    suspend fun pruneStaleBeacons(
        nowEpochMillis: Long = System.currentTimeMillis(),
        outOfRangeThresholdMillis: Long = DEFAULT_OUT_OF_RANGE_MS,
        evictThresholdMillis: Long = DEFAULT_EVICT_MS
    )

    companion object {
        const val DEFAULT_OUT_OF_RANGE_MS = 10_000L
        const val DEFAULT_EVICT_MS = 60_000L
    }
}
