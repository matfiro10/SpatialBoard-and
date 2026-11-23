package com.example.spatialboard.model

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
