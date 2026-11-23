package com.example.spatialboard.ui

import com.example.spatialboard.data.InMemorySpatialRepository
import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpatialBoardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: SpatialBoardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SpatialBoardViewModel(InMemorySpatialRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observeBoardDetailTransitionsBetweenNotFoundAndReady() = runTest {
        assertEquals(
            BoardDetailState.NotFound,
            viewModel.observeBoardDetail("board-1").first()
        )

        val board = BoardItem(
            id = "board-1",
            title = "Lab IoT",
            roomLabel = "Sala 12",
            beaconId = "bcn-1",
            updatedAt = "14:00",
            content = "Instrukcja stanowiska"
        )
        viewModel.saveBoard(board)

        val state = viewModel.observeBoardDetail("board-1").first()
        assertTrue(state is BoardDetailState.Ready)
        assertEquals(board, (state as BoardDetailState.Ready).board)

        viewModel.deleteBoard("board-1")
        assertEquals(
            BoardDetailState.NotFound,
            viewModel.observeBoardDetail("board-1").first()
        )
    }

    @Test
    fun onBeaconsScannedUpdatesStateFlowInMemory() = runTest {
        val beacon = BeaconItem(
            id = "bcn-1",
            uuid = "f7826da6-4fa2-4e98-8024-bc5b71e0893e",
            major = 1,
            minor = 5,
            name = "Beacon Wejście",
            macAddress = "11:22:33:44:55:66",
            zoneLabel = "Wejście główne",
            distanceMeters = 0.8f,
            rssiDbm = -52,
            txPowerDbm = -59,
            lastSeenLabel = "teraz"
        )

        viewModel.onBeaconsScanned(listOf(beacon))

        val emitted = viewModel.uiState.first { it.beacons.isNotEmpty() }.beacons
        assertEquals(1, emitted.size)
        assertEquals("Beacon Wejście", emitted.first().name)
    }
}
