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

class InMemorySpatialRepository : SpatialRepository {

    private val boardsFlow = MutableStateFlow<List<BoardItem>>(emptyList())
    private val beaconsFlow = MutableStateFlow<List<BeaconItem>>(emptyList())

    override fun observeAllBoards(): Flow<List<BoardItem>> = boardsFlow.asStateFlow()

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

    override fun observeBoardsForAr(beaconIds: List<String>): Flow<List<BoardItem>> {
        val idSet = beaconIds.toSet()
        return boardsFlow.map { list ->
            list.filter { it.beaconId == null || it.beaconId in idSet }
        }
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

    override suspend fun replaceBoards(boards: List<BoardItem>) {
        boardsFlow.value = boards
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
