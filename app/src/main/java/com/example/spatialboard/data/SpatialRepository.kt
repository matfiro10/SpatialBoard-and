package com.example.spatialboard.data

import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem
import com.example.spatialboard.ui.overlays.SampleSpatialData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface SpatialRepository {
    fun observeBoards(): Flow<List<BoardItem>>
    fun observeBeacons(): Flow<List<BeaconItem>>
    suspend fun upsertBoard(board: BoardItem)
    suspend fun deleteBoard(boardId: String)
    suspend fun updateBeacons(beacons: List<BeaconItem>)
}

class InMemorySpatialRepository : SpatialRepository {

    private val boardsFlow = MutableStateFlow(SampleSpatialData.initialBoards)
    private val beaconsFlow = MutableStateFlow(SampleSpatialData.initialBeacons)

    override fun observeBoards(): Flow<List<BoardItem>> = boardsFlow.asStateFlow()

    override fun observeBeacons(): Flow<List<BeaconItem>> = beaconsFlow.asStateFlow()

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

    override suspend fun updateBeacons(beacons: List<BeaconItem>) {
        beaconsFlow.value = beacons
    }
}
