package com.example.spatialboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.spatialboard.SpatialBoardApplication
import com.example.spatialboard.ble.BeaconProximityStabilizer
import com.example.spatialboard.ble.DetectedBeacon
import com.example.spatialboard.ble.toBeaconItem
import com.example.spatialboard.data.InMemorySpatialRepository
import com.example.spatialboard.data.SpatialRepository
import com.example.spatialboard.data.SpatialSettingsStore
import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed interface BoardDetailState {
    data object Loading : BoardDetailState
    data class Ready(val board: BoardItem) : BoardDetailState
    data object NotFound : BoardDetailState
}

class SpatialBoardViewModel(
    private val repository: SpatialRepository = InMemorySpatialRepository(),
    private val settingsStore: SpatialSettingsStore? = null,
    private val beaconStabilizer: BeaconProximityStabilizer = BeaconProximityStabilizer()
) : ViewModel() {

    val boardsPagingFlow: Flow<PagingData<BoardItem>> = repository
        .observeBoardsPaged()
        .cachedIn(viewModelScope)

    val beacons: StateFlow<List<BeaconItem>> = repository
        .observeBeacons()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val minRssiThreshold: StateFlow<Int> = settingsStore?.minRssiThreshold
        ?: MutableStateFlow(SpatialSettingsStore.DEFAULT_MIN_RSSI).asStateFlow()

    private val _rawDetectedBeacons = MutableStateFlow<List<DetectedBeacon>>(emptyList())

    val activeBeacons: StateFlow<List<DetectedBeacon>> = combine(
        _rawDetectedBeacons,
        minRssiThreshold
    ) { detected, minRssi ->
        beaconStabilizer.filterAndStabilize(detected, minRssi)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableArBoards: StateFlow<List<BoardItem>> = combine(
        activeBeacons,
        activeBeacons
            .map { list -> list.map { it.id }.distinct().sorted() }
            .distinctUntilChanged()
            .flatMapLatest { activeIds ->
                repository.observeBoardsForAr(activeIds)
            }
    ) { stabilizedBeacons, boards ->
        val priorityByBeaconId = stabilizedBeacons
            .mapIndexed { index, beacon -> beacon.id to index }
            .toMap()

        boards.sortedWith(
            compareBy<BoardItem>(
                { board -> if (board.beaconId != null && board.beaconId in priorityByBeaconId) 0 else 1 },
                { board -> board.beaconId?.let { priorityByBeaconId[it] } ?: Int.MAX_VALUE }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    private val _pendingArBoard = MutableStateFlow<BoardItem?>(null)
    val pendingArBoard: StateFlow<BoardItem?> = _pendingArBoard.asStateFlow()

    init {
        viewModelScope.launch {
            activeBeacons.collect { stabilized ->
                repository.upsertBeacons(stabilized.map { it.toBeaconItem() })
                repository.pruneStaleBeacons()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(2_000L.milliseconds)
                repository.pruneStaleBeacons()
            }
        }
    }

    fun setMinRssiThreshold(rssi: Int) {
        settingsStore?.setMinRssiThreshold(rssi)
    }

    fun updateDetectedBeacons(detectedBeacons: List<DetectedBeacon>) {
        _rawDetectedBeacons.value = detectedBeacons
    }

    fun selectBoardForAr(board: BoardItem?) {
        _pendingArBoard.value = board
    }

    fun observeBoardDetail(boardId: String): Flow<BoardDetailState> {
        return repository.observeBoardById(boardId).map { board ->
            if (board != null) {
                BoardDetailState.Ready(board)
            } else {
                BoardDetailState.NotFound
            }
        }
    }

    fun observeBeacon(beaconId: String): Flow<BeaconItem?> {
        return repository.observeBeaconById(beaconId)
    }

    fun saveBoard(board: BoardItem) {
        viewModelScope.launch {
            repository.upsertBoard(board)
        }
    }

    fun syncBoardsFromServer(boards: List<BoardItem>) {
        viewModelScope.launch {
            repository.upsertBoards(boards)
        }
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch {
            repository.deleteBoard(boardId)
        }
    }

    fun clearDetectedBeacons() {
        _rawDetectedBeacons.value = emptyList()
        beaconStabilizer.reset()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                val repository = (application as? SpatialBoardApplication)?.repository
                    ?: InMemorySpatialRepository()
                val settingsStore = application?.let { SpatialSettingsStore(it) }
                SpatialBoardViewModel(
                    repository = repository,
                    settingsStore = settingsStore
                )
            }
        }
    }
}
