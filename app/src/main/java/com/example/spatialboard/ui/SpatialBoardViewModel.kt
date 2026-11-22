package com.example.spatialboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.spatialboard.SpatialBoardApplication
import com.example.spatialboard.data.InMemorySpatialRepository
import com.example.spatialboard.data.SpatialRepository
import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem
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
import kotlinx.coroutines.launch

sealed interface BoardDetailState {
    data object Loading : BoardDetailState
    data class Ready(val board: BoardItem) : BoardDetailState
    data object NotFound : BoardDetailState
}

class SpatialBoardViewModel(
    private val repository: SpatialRepository = InMemorySpatialRepository()
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

    val availableArBoards: StateFlow<List<BoardItem>> = combine(
        beacons,
        beacons
            .map { list -> list.filter { it.isInRange }.map { it.id }.distinct().sorted() }
            .distinctUntilChanged()
            .flatMapLatest { activeIds ->
                repository.observeBoardsForAr(activeIds)
            }
    ) { currentBeacons, boards ->
        val distanceByBeaconId = currentBeacons
            .filter { it.isInRange }
            .associate { it.id to it.distanceMeters }

        boards.sortedWith(
            compareBy<BoardItem>(
                { board -> if (board.beaconId != null && board.beaconId in distanceByBeaconId) 0 else 1 },
                { board -> board.beaconId?.let { distanceByBeaconId[it] } ?: Float.MAX_VALUE }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    private val _pendingArBoard = MutableStateFlow<BoardItem?>(null)
    val pendingArBoard: StateFlow<BoardItem?> = _pendingArBoard.asStateFlow()

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

    fun onBeaconsScanned(scannedBeacons: List<BeaconItem>) {
        viewModelScope.launch {
            repository.upsertBeacons(scannedBeacons)
        }
    }

    fun pruneStaleBeacons() {
        viewModelScope.launch {
            repository.pruneStaleBeacons()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                val repository = (application as? SpatialBoardApplication)?.repository
                    ?: InMemorySpatialRepository()
                SpatialBoardViewModel(repository)
            }
        }
    }
}
