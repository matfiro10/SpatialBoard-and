package com.example.spatialboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spatialboard.data.InMemorySpatialRepository
import com.example.spatialboard.data.SpatialRepository
import com.example.spatialboard.ui.overlays.BeaconItem
import com.example.spatialboard.ui.overlays.BoardItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpatialBoardViewModel(
    private val repository: SpatialRepository = InMemorySpatialRepository()
) : ViewModel() {

    val boards: StateFlow<List<BoardItem>> = repository.observeBoards()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val beacons: StateFlow<List<BeaconItem>> = combine(
        repository.observeBeacons(),
        repository.observeBoards()
    ) { detectedBeacons, allBoards ->
        val countsByBeacon = allBoards
            .mapNotNull { it.beaconId }
            .groupingBy { it }
            .eachCount()

        detectedBeacons.map { beacon ->
            beacon.copy(linkedBoardsCount = countsByBeacon[beacon.id] ?: 0)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    fun saveBoard(board: BoardItem) {
        viewModelScope.launch {
            repository.upsertBoard(board)
        }
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch {
            repository.deleteBoard(boardId)
        }
    }

    fun onBeaconsScanned(scannedBeacons: List<BeaconItem>) {
        viewModelScope.launch {
            repository.updateBeacons(scannedBeacons)
        }
    }

    companion object {
        fun provideFactory(repository: SpatialRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SpatialBoardViewModel(repository) as T
                }
            }
        }
    }
}
