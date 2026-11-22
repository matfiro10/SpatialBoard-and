package com.example.spatialboard.ble

enum class BleRangePreset(val approxMeters: Int) {
    IMMEDIATE(1),
    NEAR(3),
    ROOM(6),
    WIDE(12);

    companion object {
        fun fromRssi(rssi: Int): BleRangePreset = when {
            rssi >= -62 -> IMMEDIATE
            rssi >= -73 -> NEAR
            rssi >= -84 -> ROOM
            else -> WIDE
        }
    }
}

class BeaconProximityStabilizer(
    private val exitHysteresisDb: Int = 4,
    private val switchMarginDb: Int = 5,
    private val windowSize: Int = 5
) {
    private var lockedPrimaryId: String? = null
    private val rssiHistory = HashMap<String, ArrayDeque<Int>>()

    fun filterAndStabilize(
        beacons: List<DetectedBeacon>,
        minRssiThreshold: Int
    ): List<DetectedBeacon> {
        val activeIds = beacons.mapTo(HashSet()) { it.id }
        rssiHistory.keys.retainAll(activeIds)

        val smoothedBeacons = beacons.map { beacon ->
            val history = rssiHistory.getOrPut(beacon.id) { ArrayDeque(windowSize) }
            if (history.size >= windowSize) {
                history.removeFirst()
            }
            history.addLast(beacon.rssi)
            val sortedSamples = history.sorted()
            val medianRssi = sortedSamples[sortedSamples.size / 2]
            beacon.copy(rssi = medianRssi)
        }

        val inRange = smoothedBeacons.filter { beacon ->
            val effectiveThreshold = if (beacon.id == lockedPrimaryId) {
                minRssiThreshold - exitHysteresisDb
            } else {
                minRssiThreshold
            }
            beacon.rssi >= effectiveThreshold
        }

        if (inRange.isEmpty()) {
            lockedPrimaryId = null
            return emptyList()
        }

        val sorted = inRange.sortedByDescending { it.rssi }
        val strongest = sorted.first()
        val currentLocked = lockedPrimaryId?.let { id ->
            sorted.firstOrNull { it.id == id }
        }

        val activePrimary = when {
            currentLocked == null -> strongest
            strongest.id == currentLocked.id -> currentLocked
            strongest.rssi >= currentLocked.rssi + switchMarginDb -> strongest
            else -> currentLocked
        }

        lockedPrimaryId = activePrimary.id

        return buildList(sorted.size) {
            add(activePrimary)
            sorted.forEach { beacon ->
                if (beacon.id != activePrimary.id) {
                    add(beacon)
                }
            }
        }
    }

    fun reset() {
        lockedPrimaryId = null
        rssiHistory.clear()
    }
}
