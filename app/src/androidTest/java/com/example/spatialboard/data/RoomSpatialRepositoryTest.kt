package com.example.spatialboard.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.spatialboard.data.local.SpatialDatabase
import com.example.spatialboard.model.BoardItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSpatialRepositoryTest {

    private lateinit var database: SpatialDatabase
    private lateinit var repository: RoomSpatialRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SpatialDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomSpatialRepository(database.boardDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndDeleteBoardPersistsInDatabase() = runTest {
        val board = BoardItem(
            id = "b-1",
            title = "Harmonogram",
            roomLabel = "Sala 204",
            beaconId = "beacon-204",
            updatedAt = "10:15",
            content = "Treść tablicy",
            tags = listOf("Plan")
        )

        repository.upsertBoard(board)
        assertEquals(board, repository.observeBoardById("b-1").first())

        repository.deleteBoard("b-1")
        assertNull(repository.observeBoardById("b-1").first())
    }

    @Test
    fun observeBoardsForArFiltersCorrectly() = runTest {
        val boardWithBeacon = BoardItem(
            id = "b-1",
            title = "Tablica Sala 1",
            roomLabel = "Sala 1",
            beaconId = "beacon-1",
            updatedAt = "10:00",
            content = "Opis"
        )
        val boardWithoutBeacon = BoardItem(
            id = "b-2",
            title = "Tablica Ogólna",
            roomLabel = "Korytarz",
            beaconId = null,
            updatedAt = "10:05",
            content = "Opis"
        )
        val boardOtherBeacon = BoardItem(
            id = "b-3",
            title = "Tablica Sala 2",
            roomLabel = "Sala 2",
            beaconId = "beacon-2",
            updatedAt = "10:10",
            content = "Opis"
        )

        repository.upsertBoard(boardWithBeacon)
        repository.upsertBoard(boardWithoutBeacon)
        repository.upsertBoard(boardOtherBeacon)
git
        val visibleBoards = repository.observeBoardsForAr(listOf("beacon-1")).first()
        val visibleIds = visibleBoards.map { it.id }.toSet()

        assertEquals(2, visibleBoards.size)
        assertEquals(setOf("b-1", "b-2"), visibleIds)
    }
}
