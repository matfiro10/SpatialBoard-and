package com.example.spatialboard.ble

import com.example.spatialboard.model.BeaconItem
import kotlin.math.pow
import kotlin.math.round

data class DetectedBeacon(
    val id: String,
    val uuid: String,
    val major: Int,
    val minor: Int,
    val name: String,
    val macAddress: String,
    val rssi: Int,
    val txPower: Int,
    val lastSeenMs: Long
)

fun DetectedBeacon.toBeaconItem(
    zoneLabel: String = resolveZoneLabel(rssi),
    lastSeenLabel: String = "Teraz"
): BeaconItem {
    val rawMeters = 10.0.pow((txPower - rssi) / 20.0).toFloat()
    val roundedMeters = round(rawMeters * 10f) / 10f

    return BeaconItem(
        id = id,
        uuid = uuid,
        major = major,
        minor = minor,
        name = name,
        macAddress = macAddress,
        zoneLabel = zoneLabel,
        distanceMeters = roundedMeters,
        rssiDbm = rssi,
        txPowerDbm = txPower,
        lastSeenLabel = lastSeenLabel,
        isInRange = true,
        lastSeenEpochMillis = System.currentTimeMillis()
    )
}

private fun resolveZoneLabel(rssi: Int): String = when {
    rssi >= -60 -> "Bardzo blisko"
    rssi >= -75 -> "W pobliżu"
    else -> "Daleko"
}
