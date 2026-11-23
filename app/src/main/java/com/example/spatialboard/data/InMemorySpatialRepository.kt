package com.example.spatialboard.data

import androidx.paging.PagingData
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemorySpatialRepository : SpatialRepository {

    private val boardsFlow = MutableStateFlow<List<BoardItem>>(emptyList())

    override fun observeAllBoards(): Flow<List<BoardItem>> = boardsFlow.asStateFlow()

    override fun observeBoardsPaged(): Flow<PagingData<BoardItem>> {
        return boardsFlow.map { PagingData.from(it) }
    }

    override fun observeBoardById(boardId: String): Flow<BoardItem?> {
        return boardsFlow
            .map { list -> list.find { it.id == boardId } }
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

    override suspend fun deleteBoard(boardId: String) {
        boardsFlow.update { currentList ->
            currentList.filterNot { it.id == boardId }
        }
    }

    fun setBoards(boards: List<BoardItem>) {
        boardsFlow.value = boards
    }

    fun getCurrentBoards(): List<BoardItem> = boardsFlow.value
}
