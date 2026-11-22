package com.example.spatialboard.data

import androidx.paging.PagingData
import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface SpatialRepository {
    fun observeBoardsPaged(): Flow<PagingData<BoardItem>>
    fun observeBoardById(boardId: String): Flow<BoardItem?>
    fun observeBeacons(): Flow<List<BeaconItem>>
    fun observeBeaconById(beaconId: String): Flow<BeaconItem?>
    suspend fun upsertBoard(board: BoardItem)
    suspend fun upsertBoards(boards: List<BoardItem>)
    suspend fun deleteBoard(boardId: String)
    suspend fun upsertBeacons(scannedBeacons: List<BeaconItem>)
    suspend fun pruneStaleBeacons(
        nowEpochMillis: Long = System.currentTimeMillis(),
        outOfRangeThresholdMillis: Long = 6_000L,
        evictThresholdMillis: Long = 30_000L
    )
}

class InMemorySpatialRepository : SpatialRepository {

    private val boardsFlow = MutableStateFlow<List<BoardItem>>(emptyList())
    private val beaconsFlow = MutableStateFlow<List<BeaconItem>>(emptyList())

    override fun observeBoardsPaged(): Flow<PagingData<BoardItem>> {
        return boardsFlow.map { PagingData.from(it) }
    }

    override fun observeBoardById(boardId: String): Flow<BoardItem?> {
        return boardsFlow
            .map { list -> list.find { it.id == boardId } }
            .distinctUntilChanged()
    }

    override fun observeBeacons(): Flow<List<BeaconItem>> = beaconsFlow.asStateFlow()

    override fun observeBeaconById(beaconId: String): Flow<BeaconItem?> {
        return beaconsFlow
            .map { list -> list.find { it.id == beaconId } }
            .distinctUntilChanged()
    }

    override suspend fun upsertBoard(board: BoardItem) {
        boardsFlow.update { currentList ->
            val exists = currentList.any { it.id == board.id }
            if (exists) {
                currentList.map { item -> if (item.id == board.id) board else item }
            } else {
                listOf(board) + currentList
            }
        }
    }

    override suspend fun upsertBoards(boards: List<BoardItem>) {
        boards.forEach { upsertBoard(it) }
    }

    override suspend fun deleteBoard(boardId: String) {
        boardsFlow.update { currentList ->
            currentList.filterNot { it.id == boardId }
        }
    }

    override suspend fun upsertBeacons(scannedBeacons: List<BeaconItem>) {
        if (scannedBeacons.isEmpty()) return
        beaconsFlow.update { currentList ->
            val merged = currentList.associateByTo(LinkedHashMap()) { it.id }
            scannedBeacons.forEach { incoming ->
                merged[incoming.id] = incoming.copy(isInRange = true)
            }
            merged.values.toList()
        }
    }

    override suspend fun pruneStaleBeacons(
        nowEpochMillis: Long,
        outOfRangeThresholdMillis: Long,
        evictThresholdMillis: Long
    ) {
        beaconsFlow.update { currentList ->
            currentList.mapNotNull { beacon ->
                val ageMillis = nowEpochMillis - beacon.lastSeenEpochMillis
                when {
                    ageMillis > evictThresholdMillis -> null
                    ageMillis > outOfRangeThresholdMillis -> beacon.copy(isInRange = false)
                    else -> beacon
                }
            }
        }
    }
}
