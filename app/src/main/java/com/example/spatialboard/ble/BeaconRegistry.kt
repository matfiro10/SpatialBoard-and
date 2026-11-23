package com.example.spatialboard.ble

import com.example.spatialboard.ui.overlays.BeaconItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class BeaconRegistry(
    private val outOfRangeThresholdMillis: Long = DEFAULT_OUT_OF_RANGE_MS,
    private val evictThresholdMillis: Long = DEFAULT_EVICT_MS
) {
    private val _beaconsFlow = MutableStateFlow<List<BeaconItem>>(emptyList())
    val beacons: Flow<List<BeaconItem>> = _beaconsFlow.asStateFlow()

    fun observeBeaconById(beaconId: String): Flow<BeaconItem?> {
        return _beaconsFlow
            .map { list -> list.find { it.id == beaconId } }
            .distinctUntilChanged()
    }

    fun upsert(scannedBeacons: List<BeaconItem>) {
        if (scannedBeacons.isEmpty()) return
        _beaconsFlow.update { currentList ->
            val merged = currentList.associateByTo(LinkedHashMap()) { it.id }
            scannedBeacons.forEach { incoming ->
                merged[incoming.id] = incoming.copy(isInRange = true)
            }
            merged.values.toList()
        }
    }

    fun pruneStale(nowEpochMillis: Long = System.currentTimeMillis()) {
        _beaconsFlow.update { currentList ->
            currentList.mapNotNull { beacon ->
                val ageMillis = nowEpochMillis - beacon.lastSeenEpochMillis
                when {
                    ageMillis > evictThresholdMillis -> null
                    ageMillis > outOfRangeThresholdMillis -> beacon.copy(isInRange = false)
                    else -> beacon
                }
            }
        }
    }

    fun clear() {
        _beaconsFlow.value = emptyList()
    }

    companion object {
        const val DEFAULT_OUT_OF_RANGE_MS = 10_000L
        const val DEFAULT_EVICT_MS = 60_000L
    }
}