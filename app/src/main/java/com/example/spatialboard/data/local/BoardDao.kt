package com.example.spatialboard.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM boards ORDER BY updatedAtEpoch DESC")
    fun pagingSource(): PagingSource<Int, BoardEntity>

    @Query("SELECT * FROM boards WHERE id = :boardId")
    fun observeBoardById(boardId: String): Flow<BoardEntity?>

    @Upsert
    suspend fun upsertBoard(board: BoardEntity)

    @Upsert
    suspend fun upsertBoards(boards: List<BoardEntity>)

    @Query("DELETE FROM boards WHERE id = :boardId")
    suspend fun deleteBoardById(boardId: String)
}
