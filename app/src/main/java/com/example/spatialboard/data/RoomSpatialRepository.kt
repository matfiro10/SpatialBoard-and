package com.example.spatialboard.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.spatialboard.data.local.BoardDao
import com.example.spatialboard.data.local.toDomain
import com.example.spatialboard.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.example.spatialboard.model.BoardItem

class RoomSpatialRepository(
    private val boardDao: BoardDao
) : SpatialRepository {

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

    override fun observeBoardsForAr(beaconIds: List<String>): Flow<List<BoardItem>> {
        return boardDao.observeBoardsForAr(beaconIds).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsertBoard(board: BoardItem) {
        boardDao.upsertBoard(board.toEntity())
    }

    override suspend fun deleteBoard(boardId: String) {
        boardDao.deleteBoardById(boardId)
    }
}
