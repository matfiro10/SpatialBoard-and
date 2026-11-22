package com.example.spatialboard.ui.overlays

data class BoardItem(
    val id: String,
    val title: String,
    val roomLabel: String,
    val beaconId: String?,
    val updatedAt: String,
    val content: String,
    val mediaUri: String? = null,
    val tags: List<String> = emptyList()
)

data class BeaconItem(
    val id: String,
    val uuid: String,
    val major: Int,
    val minor: Int,
    val name: String,
    val macAddress: String,
    val zoneLabel: String,
    val distanceMeters: Float,
    val rssiDbm: Int,
    val txPowerDbm: Int,
    val lastSeenLabel: String,
    val linkedBoardsCount: Int = 0,
    val isInRange: Boolean = true,
    val lastSeenEpochMillis: Long = System.currentTimeMillis()
)
