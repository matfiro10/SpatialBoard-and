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
import com.example.spatialboard.ble.BeaconRegistry
import com.example.spatialboard.ble.DetectedBeacon
import com.example.spatialboard.ble.toBeaconItem
import com.example.spatialboard.data.DataSourceMode
import com.example.spatialboard.data.InMemorySpatialRepository
import com.example.spatialboard.data.SpatialRepository
import com.example.spatialboard.data.SpatialSettingsStore
import com.example.spatialboard.data.remote.RemoteSpatialRepository
import com.example.spatialboard.data.remote.ServerSyncState
import com.example.spatialboard.model.BeaconItem
import com.example.spatialboard.model.BoardItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.collections.sortedWith
import kotlin.comparisons.compareBy
import kotlin.time.Duration.Companion.milliseconds

sealed interface BoardDetailState {
    data object Loading : BoardDetailState
    data class Ready(val board: BoardItem) : BoardDetailState
    data object NotFound : BoardDetailState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SpatialBoardViewModel(
    private val localRepository: SpatialRepository = InMemorySpatialRepository(),
    private val serverRepository: RemoteSpatialRepository? = null,
    private val beaconRegistry: BeaconRegistry = BeaconRegistry(),
    private val settingsStore: SpatialSettingsStore? = null,
    private val beaconStabilizer: BeaconProximityStabilizer = BeaconProximityStabilizer()
) : ViewModel() {

    private val serverConfigFlow: Flow<ServerConfig> = combine(
        settingsStore?.serverConfig ?: flowOf(ServerConfig()),
        serverRepository?.syncState ?: flowOf(ServerSyncState.Idle)
    ) { config, syncState ->
        config.copy(syncState = syncState)
    }

    private val minRssiThresholdFlow: Flow<Int> =
        settingsStore?.minRssiThreshold ?: flowOf(SpatialSettingsStore.DEFAULT_MIN_RSSI)

    private val _rawDetectedBeacons = MutableStateFlow<List<DetectedBeacon>>(emptyList())
    private val _pendingArBoard = MutableStateFlow<BoardItem?>(null)

    val boardsPagingFlow: Flow<PagingData<BoardItem>> = serverConfigFlow
        .map { it.mode }
        .distinctUntilChanged()
        .flatMapLatest { mode -> repositoryFor(mode).observeBoardsPaged() }
        .cachedIn(viewModelScope)

    private val activeSourceBoards: Flow<List<BoardItem>> = serverConfigFlow
        .map { it.mode }
        .distinctUntilChanged()
        .flatMapLatest { mode -> repositoryFor(mode).observeAllBoards() }

    private val activeBeaconsFlow: Flow<List<DetectedBeacon>> = combine(
        _rawDetectedBeacons,
        minRssiThresholdFlow
    ) { detected, minRssi ->
        beaconStabilizer.filterAndStabilize(detected, minRssi)
    }

    private val beaconsFlow: Flow<List<BeaconItem>> = combine(
        beaconRegistry.beacons,
        activeSourceBoards
    ) { detectedBeacons, boards ->
        val countsByBeaconId = boards
            .mapNotNull { it.beaconId }
            .groupingBy { it }
            .eachCount()

        detectedBeacons.map { beacon ->
            beacon.copy(linkedBoardsCount = countsByBeaconId[beacon.id] ?: 0)
        }
    }

    private val availableArBoardsFlow: Flow<List<BoardItem>> = combine(
        activeBeaconsFlow,
        combine(
            activeBeaconsFlow
                .map { list -> list.map { it.id }.distinct().sorted() }
                .distinctUntilChanged(),
            serverConfigFlow.map { it.mode }.distinctUntilChanged()
        ) { activeIds, mode ->
            activeIds to mode
        }.flatMapLatest { (activeIds, mode) ->
            repositoryFor(mode).observeBoardsForAr(activeIds)
        }
    ) { stabilizedBeacons, boards ->
        val priorityByBeaconId = stabilizedBeacons
            .mapIndexed { index, beacon -> beacon.id to index }
            .toMap()

        boards.sortedWith(
            compareBy(
                { board -> if (board.beaconId != null && board.beaconId in priorityByBeaconId) 0 else 1 },
                { board -> board.beaconId?.let { priorityByBeaconId[it] } ?: Int.MAX_VALUE }
            )
        )
    }

    private val arStateFlow: Flow<Pair<List<BoardItem>, BoardItem?>> = combine(
        availableArBoardsFlow,
        _pendingArBoard
    ) { available, pending -> available to pending }

    val uiState: StateFlow<SpatialBoardUiState> = combine(
        serverConfigFlow,
        minRssiThresholdFlow,
        beaconsFlow,
        activeBeaconsFlow,
        arStateFlow
    ) { serverConfig, minRssi, beacons, activeBeacons, (arBoards, pendingBoard) ->
        SpatialBoardUiState(
            serverConfig = serverConfig,
            minRssiThreshold = minRssi,
            beacons = beacons,
            activeBeacons = activeBeacons,
            availableArBoards = arBoards,
            pendingArBoard = pendingBoard
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SpatialBoardUiState()
    )

    init {
        viewModelScope.launch {
            uiState.map { it.activeBeacons }
                .distinctUntilChanged()
                .collect { stabilized ->
                    beaconRegistry.upsert(stabilized.map { it.toBeaconItem() })
                    beaconRegistry.pruneStale()
                }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(2_000L.milliseconds)
                beaconRegistry.pruneStale()
            }
        }

        viewModelScope.launch {
            combine(
                serverConfigFlow.map { it.mode to (it.host to it.port) }.distinctUntilChanged(),
                uiState.map { it.activeBeacons }.distinctUntilChanged { old, new ->
                    old.map { it.id }.toSet() == new.map { it.id }.toSet()
                }
            ) { (mode, _), beacons ->
                mode to beacons
            }.collect { (mode, beacons) ->
                if (mode == DataSourceMode.SERVER) {
                    serverRepository?.refreshForBeacons(beacons)
                }
            }
        }
    }

    private fun repositoryFor(mode: DataSourceMode): SpatialRepository = when (mode) {
        DataSourceMode.LOCAL -> localRepository
        DataSourceMode.SERVER -> serverRepository ?: localRepository
    }

    private fun activeRepository(): SpatialRepository = repositoryFor(uiState.value.dataSourceMode)

    fun setDataSourceMode(mode: DataSourceMode) {
        if (uiState.value.dataSourceMode == mode) return
        _pendingArBoard.value = null
        settingsStore?.setDataSourceMode(mode)
    }

    fun updateServerEndpoint(host: String, port: Int) {
        settingsStore?.setServerEndpoint(host, port)
    }

    fun testServerConnection() {
        viewModelScope.launch {
            serverRepository?.testConnection()
        }
    }

    fun setMinRssiThreshold(rssi: Int) {
        settingsStore?.setMinRssiThreshold(rssi)
    }

    fun updateDetectedBeacons(detectedBeacons: List<DetectedBeacon>) {
        _rawDetectedBeacons.value = detectedBeacons
    }

    fun onBeaconsScanned(beacons: List<BeaconItem>) {
        beaconRegistry.upsert(beacons)
    }

    fun clearDetectedBeacons() {
        _rawDetectedBeacons.value = emptyList()
        beaconStabilizer.reset()
    }

    fun selectBoardForAr(board: BoardItem?) {
        _pendingArBoard.value = board
    }

    fun observeBoardDetail(boardId: String): Flow<BoardDetailState> {
        return serverConfigFlow.map { it.mode }.distinctUntilChanged().flatMapLatest { mode ->
            repositoryFor(mode).observeBoardById(boardId).map { board ->
                if (board != null) {
                    BoardDetailState.Ready(board)
                } else {
                    BoardDetailState.NotFound
                }
            }
        }
    }

    fun observeBeacon(beaconId: String): Flow<BeaconItem?> {
        return combine(
            beaconRegistry.observeBeaconById(beaconId),
            activeSourceBoards
        ) { beacon, boards ->
            beacon?.copy(
                linkedBoardsCount = boards.count { it.beaconId == beacon.id }
            )
        }.distinctUntilChanged()
    }

    fun saveBoard(board: BoardItem) {
        viewModelScope.launch {
            activeRepository().upsertBoard(board)
        }
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch {
            activeRepository().deleteBoard(boardId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as? SpatialBoardApplication
                val localRepository = app?.repository ?: InMemorySpatialRepository()
                val serverRepository = app?.networkRepository
                val beaconRegistry = app?.beaconRegistry ?: BeaconRegistry()
                val settingsStore = app?.settingsStore
                SpatialBoardViewModel(
                    localRepository = localRepository,
                    serverRepository = serverRepository,
                    beaconRegistry = beaconRegistry,
                    settingsStore = settingsStore
                )
            }
        }
    }
}