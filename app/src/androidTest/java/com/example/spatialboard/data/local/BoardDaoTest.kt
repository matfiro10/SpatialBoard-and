package com.example.spatialboard.data.local

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardDaoTest {

    private lateinit var database: SpatialDatabase
    private lateinit var boardDao: BoardDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SpatialDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        boardDao = database.boardDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndObserveBoardByIdReturnsSavedEntity() = runTest {
        val entity = createBoardEntity(
            id = "board-1",
            title = "Tablica Lab 101",
            beaconId = "beacon-uuid-1",
            tags = listOf("IoT", "AR")
        )

        boardDao.upsertBoard(entity)
        val loaded = boardDao.observeBoardById("board-1").first()

        assertEquals(entity, loaded)
    }

    @Test
    fun observeNonExistentBoardReturnsNull() = runTest {
        val loaded = boardDao.observeBoardById("missing-id").first()
        assertNull(loaded)
    }

    @Test
    fun upsertWithExistingIdUpdatesRecordWithoutDuplicating() = runTest {
        val initial = createBoardEntity(id = "board-1", title = "Stary tytuł", updatedAtEpoch = 1000L)
        val updated = createBoardEntity(id = "board-1", title = "Nowy tytuł", updatedAtEpoch = 2000L)

        boardDao.upsertBoard(initial)
        boardDao.upsertBoard(updated)

        val page = boardDao.pagingSource().load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertEquals(1, page.data.size)
        assertEquals("Nowy tytuł", page.data.first().title)
        assertEquals(2000L, page.data.first().updatedAtEpoch)
    }

    @Test
    fun nullableFieldsAndEmptyTagsPersistProperly() = runTest {
        val entity = createBoardEntity(
            id = "board-nulls",
            beaconId = null,
            mediaUri = null,
            tags = emptyList()
        )

        boardDao.upsertBoard(entity)
        val loaded = boardDao.observeBoardById("board-nulls").first()

        assertNotNull(loaded)
        assertNull(loaded?.beaconId)
        assertNull(loaded?.mediaUri)
        assertTrue(loaded?.tags?.isEmpty() == true)
    }

    @Test
    fun pagingSourceReturnsEmptyPageWhenTableIsEmpty() = runTest {
        val result = boardDao.pagingSource().load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 15,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertTrue(result.data.isEmpty())
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun pagingSourceOrdersByUpdatedAtDescAndPaginatesCorrectly() = runTest {
        val entities = (1..25).map { index ->
            createBoardEntity(
                id = "board-$index",
                title = "Board $index",
                updatedAtEpoch = index * 1000L
            )
        }
        boardDao.upsertBoards(entities)

        val pagingSource = boardDao.pagingSource()

        val firstPage = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertEquals(10, firstPage.data.size)
        assertEquals("board-25", firstPage.data.first().id)
        assertEquals("board-16", firstPage.data.last().id)
        assertNotNull(firstPage.nextKey)

        val secondPage = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = firstPage.nextKey!!,
                loadSize = 10,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertEquals(10, secondPage.data.size)
        assertEquals("board-15", secondPage.data.first().id)
        assertEquals("board-6", secondPage.data.last().id)
        assertNotNull(secondPage.nextKey)

        val thirdPage = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = secondPage.nextKey!!,
                loadSize = 10,
                placeholdersEnabled = false
            )
        ) as PagingSource.LoadResult.Page

        assertEquals(5, thirdPage.data.size)
        assertEquals("board-5", thirdPage.data.first().id)
        assertEquals("board-1", thirdPage.data.last().id)
        assertNull(thirdPage.nextKey)
    }

    @Test
    fun deleteBoardByIdRemovesOnlyTargetRecord() = runTest {
        boardDao.upsertBoards(
            listOf(
                createBoardEntity(id = "board-1"),
                createBoardEntity(id = "board-2")
            )
        )

        boardDao.deleteBoardById("board-1")
        boardDao.deleteBoardById("non-existent-id")

        assertNull(boardDao.observeBoardById("board-1").first())
        assertNotNull(boardDao.observeBoardById("board-2").first())
    }

    private fun createBoardEntity(
        id: String,
        title: String = "Title $id",
        roomLabel: String = "Room A",
        beaconId: String? = "beacon-1",
        updatedAt: String = "12:00",
        content: String = "Content",
        mediaUri: String? = "https://example.com/image.png",
        tags: List<String> = listOf("tag1"),
        updatedAtEpoch: Long = 1000L
    ): BoardEntity = BoardEntity(
        id = id,
        title = title,
        roomLabel = roomLabel,
        beaconId = beaconId,
        updatedAt = updatedAt,
        content = content,
        mediaUri = mediaUri,
        tags = tags,
        updatedAtEpoch = updatedAtEpoch
    )
}
