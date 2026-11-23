package com.example.spatialboard.data

import androidx.paging.PagingData
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.flow.Flow

interface SpatialRepository {
    fun observeAllBoards(): Flow<List<BoardItem>>
    fun observeBoardsPaged(): Flow<PagingData<BoardItem>>
    fun observeBoardById(boardId: String): Flow<BoardItem?>
    fun observeBoardsForAr(beaconIds: List<String>): Flow<List<BoardItem>>

    suspend fun upsertBoard(board: BoardItem)
    suspend fun upsertBoards(boards: List<BoardItem>)
    suspend fun replaceBoards(boards: List<BoardItem>)
    suspend fun deleteBoard(boardId: String)
}