package com.example.spatialboard.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.spatialboard.data.local.BoardDao
import com.example.spatialboard.data.local.toDomain
import com.example.spatialboard.data.local.toEntity
import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class RoomSpatialRepository(
    private val boardDao: BoardDao
) : SpatialRepository {

    private val beaconsFlow = MutableStateFlow<List<BeaconItem>>(emptyList())

    override fun observeAllBoards(): Flow<List<BoardItem>> {
        return boardDao.observeAllBoards().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeBoardsPaged(): Flow<PagingData<BoardItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 15,
                initialLoadSize = 30,
                prefetchDistance = 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { boardDao.pagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun observeBoardById(boardId: String): Flow<BoardItem?> {
        return boardDao.observeBoardById(boardId)
            .map { entity -> entity?.toDomain() }
            .distinctUntilChanged()
    }

    override fun observeBeacons(): Flow<List<BeaconItem>> = beaconsFlow.asStateFlow()

    override fun observeBeaconById(beaconId: String): Flow<BeaconItem?> {
        return beaconsFlow
            .map { list -> list.find { it.id == beaconId } }
            .distinctUntilChanged()
    }

    override fun observeBoardsForAr(beaconIds: List<String>): Flow<List<BoardItem>> {
        return boardDao.observeBoardsForAr(beaconIds).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsertBoard(board: BoardItem) {
        boardDao.upsertBoard(board.toEntity())
    }

    override suspend fun upsertBoards(boards: List<BoardItem>) {
        val baseTime = System.currentTimeMillis()
        val entities = boards.mapIndexed { index, board ->
            board.toEntity(updatedAtEpoch = baseTime - index)
        }
        boardDao.upsertBoards(entities)
    }

    override suspend fun replaceBoards(boards: List<BoardItem>) {
        boardDao.deleteAllBoards()
        if (boards.isNotEmpty()) {
            upsertBoards(boards)
        }
    }

    override suspend fun deleteBoard(boardId: String) {
        boardDao.deleteBoardById(boardId)
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
