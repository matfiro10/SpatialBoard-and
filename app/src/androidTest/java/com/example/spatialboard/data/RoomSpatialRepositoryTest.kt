package com.example.spatialboard.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.spatialboard.data.local.SpatialDatabase
import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun upsertBeaconsMergesInMemoryAndIgnoresEmptyScans() = runTest {
        val beacon1 = createBeacon(id = "bcn-1", rssiDbm = -65, lastSeenEpochMillis = 10_000L)
        val beacon2 = createBeacon(id = "bcn-2", rssiDbm = -72, lastSeenEpochMillis = 10_500L)

        repository.upsertBeacons(listOf(beacon1, beacon2))
        repository.upsertBeacons(emptyList())

        assertEquals(2, repository.observeBeacons().first().size)

        val updatedBeacon1 = createBeacon(id = "bcn-1", rssiDbm = -55, lastSeenEpochMillis = 12_000L)
        repository.upsertBeacons(listOf(updatedBeacon1))

        val currentBeacons = repository.observeBeacons().first()
        assertEquals(2, currentBeacons.size)
        assertEquals(-55, repository.observeBeaconById("bcn-1").first()?.rssiDbm)
        assertTrue(repository.observeBeaconById("bcn-1").first()?.isInRange == true)
    }

    @Test
    fun pruneStaleBeaconsMarksOutOfRangeAndEvictsExpiredWithoutTouchingBoards() = runTest {
        repository.upsertBoard(
            BoardItem(
                id = "board-keep",
                title = "Stała tablica",
                roomLabel = "Hol",
                beaconId = "bcn-evict",
                updatedAt = "09:00",
                content = "Opis"
            )
        )

        val activeBeacon = createBeacon(id = "bcn-active", lastSeenEpochMillis = 97_000L)
        val staleBeacon = createBeacon(id = "bcn-stale", lastSeenEpochMillis = 90_000L)
        val expiredBeacon = createBeacon(id = "bcn-evict", lastSeenEpochMillis = 60_000L)

        repository.upsertBeacons(listOf(activeBeacon, staleBeacon, expiredBeacon))

        repository.pruneStaleBeacons(
            nowEpochMillis = 100_000L,
            outOfRangeThresholdMillis = 6_000L,
            evictThresholdMillis = 30_000L
        )

        val remaining = repository.observeBeacons().first()
        assertEquals(2, remaining.size)

        val observedActive = repository.observeBeaconById("bcn-active").first()
        val observedStale = repository.observeBeaconById("bcn-stale").first()
        val observedExpired = repository.observeBeaconById("bcn-evict").first()

        assertTrue(observedActive?.isInRange == true)
        assertFalse(observedStale?.isInRange ?: true)
        assertNull(observedExpired)
        assertNotNull(repository.observeBoardById("board-keep").first())
    }

    private fun createBeacon(
        id: String,
        rssiDbm: Int = -60,
        lastSeenEpochMillis: Long = 1000L
    ): BeaconItem = BeaconItem(
        id = id,
        uuid = "f7826da6-4fa2-4e98-8024-bc5b71e0893e",
        major = 1,
        minor = 101,
        name = "Beacon $id",
        macAddress = "AA:BB:CC:DD:EE:FF",
        zoneLabel = "Strefa A",
        distanceMeters = 1.5f,
        rssiDbm = rssiDbm,
        txPowerDbm = -59,
        lastSeenLabel = "teraz",
        isInRange = true,
        lastSeenEpochMillis = lastSeenEpochMillis
    )
}
