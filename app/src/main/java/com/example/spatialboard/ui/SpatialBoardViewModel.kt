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
import com.example.spatialboard.data.DataSourceMode
import com.example.spatialboard.data.InMemorySpatialRepository
import com.example.spatialboard.data.SpatialRepository
import com.example.spatialboard.data.SpatialSettingsStore
import com.example.spatialboard.data.remote.NetworkSpatialRepository
import com.example.spatialboard.data.remote.ServerSyncState
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

private data class SettingsSnapshot(
    val mode: DataSourceMode,
    val host: String,
    val port: Int,
    val minRssi: Int,
    val syncState: ServerSyncState
)

@OptIn(ExperimentalCoroutinesApi::class)
class SpatialBoardViewModel(
    private val localRepository: SpatialRepository = InMemorySpatialRepository(),
    private val serverRepository: SpatialRepository = InMemorySpatialRepository(),
    private val settingsStore: SpatialSettingsStore? = null,
    private val beaconStabilizer: BeaconProximityStabilizer = BeaconProximityStabilizer()
) : ViewModel() {

    private val dataSourceModeFlow: StateFlow<DataSourceMode> = settingsStore?.dataSourceMode
        ?: MutableStateFlow(DataSourceMode.LOCAL).asStateFlow()

    private val serverHostFlow: StateFlow<String> = settingsStore?.serverHost
        ?: MutableStateFlow(SpatialSettingsStore.DEFAULT_SERVER_HOST).asStateFlow()

    private val serverPortFlow: StateFlow<Int> = settingsStore?.serverPort
        ?: MutableStateFlow(SpatialSettingsStore.DEFAULT_SERVER_PORT).asStateFlow()

    private val minRssiThresholdFlow: StateFlow<Int> = settingsStore?.minRssiThreshold
        ?: MutableStateFlow(SpatialSettingsStore.DEFAULT_MIN_RSSI).asStateFlow()

    private val serverSyncStateFlow: StateFlow<ServerSyncState> =
        (serverRepository as? NetworkSpatialRepository)?.syncState
            ?: MutableStateFlow<ServerSyncState>(ServerSyncState.Idle).asStateFlow()

    private val _rawDetectedBeacons = MutableStateFlow<List<DetectedBeacon>>(emptyList())
    private val _pendingArBoard = MutableStateFlow<BoardItem?>(null)

    val boardsPagingFlow: Flow<PagingData<BoardItem>> = dataSourceModeFlow
        .flatMapLatest { mode -> repositoryFor(mode).observeBoardsPaged() }
        .cachedIn(viewModelScope)

    private val activeSourceBoards: Flow<List<BoardItem>> = dataSourceModeFlow
        .flatMapLatest { mode -> repositoryFor(mode).observeAllBoards() }

    private val activeBeaconsFlow: Flow<List<DetectedBeacon>> = combine(
        _rawDetectedBeacons,
        minRssiThresholdFlow
    ) { detected, minRssi ->
        beaconStabilizer.filterAndStabilize(detected, minRssi)
    }

    private val beaconsFlow: Flow<List<BeaconItem>> = combine(
        localRepository.observeBeacons(),
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
            dataSourceModeFlow
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

    private val settingsSnapshotFlow: Flow<SettingsSnapshot> = combine(
        dataSourceModeFlow,
        serverHostFlow,
        serverPortFlow,
        minRssiThresholdFlow,
        serverSyncStateFlow
    ) { mode, host, port, minRssi, syncState ->
        SettingsSnapshot(mode, host, port, minRssi, syncState)
    }

    val uiState: StateFlow<SpatialBoardUiState> = combine(
        settingsSnapshotFlow,
        beaconsFlow,
        activeBeaconsFlow,
        availableArBoardsFlow,
        _pendingArBoard
    ) { settings, beacons, activeBeacons, arBoards, pendingBoard ->
        SpatialBoardUiState(
            dataSourceMode = settings.mode,
            serverHost = settings.host,
            serverPort = settings.port,
            minRssiThreshold = settings.minRssi,
            serverSyncState = settings.syncState,
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
                    localRepository.upsertBeacons(stabilized.map { it.toBeaconItem() })
                    localRepository.pruneStaleBeacons()
                }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(2_000L.milliseconds)
                localRepository.pruneStaleBeacons()
            }
        }

        viewModelScope.launch {
            combine(
                dataSourceModeFlow,
                serverHostFlow,
                serverPortFlow,
                uiState.map { it.activeBeacons }.distinctUntilChanged { old, new ->
                    old.map { it.id }.toSet() == new.map { it.id }.toSet()
                }
            ) { mode, _, _, beacons ->
                mode to beacons
            }.collect { (mode, beacons) ->
                if (mode == DataSourceMode.SERVER) {
                    (serverRepository as? NetworkSpatialRepository)?.refreshForBeacons(beacons)
                }
            }
        }
    }

    private fun repositoryFor(mode: DataSourceMode): SpatialRepository = when (mode) {
        DataSourceMode.LOCAL -> localRepository
        DataSourceMode.SERVER -> serverRepository
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
            (serverRepository as? NetworkSpatialRepository)?.testConnection()
        }
    }

    fun setMinRssiThreshold(rssi: Int) {
        settingsStore?.setMinRssiThreshold(rssi)
    }

    fun updateDetectedBeacons(detectedBeacons: List<DetectedBeacon>) {
        _rawDetectedBeacons.value = detectedBeacons
    }

    fun onBeaconsScanned(beacons: List<BeaconItem>) {
        viewModelScope.launch {
            localRepository.upsertBeacons(beacons)
        }
    }

    fun clearDetectedBeacons() {
        _rawDetectedBeacons.value = emptyList()
        beaconStabilizer.reset()
    }

    fun selectBoardForAr(board: BoardItem?) {
        _pendingArBoard.value = board
    }

    fun observeBoardDetail(boardId: String): Flow<BoardDetailState> {
        return dataSourceModeFlow.flatMapLatest { mode ->
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
            localRepository.observeBeaconById(beaconId),
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
                val serverRepository = app?.networkRepository ?: InMemorySpatialRepository()
                val settingsStore = app?.settingsStore
                SpatialBoardViewModel(
                    localRepository = localRepository,
                    serverRepository = serverRepository,
                    settingsStore = settingsStore
                )
            }
        }
    }
}
