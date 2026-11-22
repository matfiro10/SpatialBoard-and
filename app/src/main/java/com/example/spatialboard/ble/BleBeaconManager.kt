package com.example.spatialboard.ble

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region

class BleBeaconScanner(context: Context) {

    private val appContext = context.applicationContext
    private val beaconManager = BeaconManager.getInstanceForApplication(appContext).apply {
        beaconParsers.clear()
        beaconParsers.add(BeaconParser().setBeaconLayout(IBEACON_LAYOUT))
        beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))
        foregroundScanPeriod = 1100L
        foregroundBetweenScanPeriod = 0L
    }

    private val region = Region("spatial-board-all", null, null, null)

    private val _beacons = MutableStateFlow<List<DetectedBeacon>>(emptyList())
    val beacons: StateFlow<List<DetectedBeacon>> = _beacons.asStateFlow()

    private val rangeNotifier = RangeNotifier { detected, _ ->
        val now = SystemClock.elapsedRealtime()
        _beacons.value = detected
            .map { it.toDetectedBeacon(now) }
            .sortedByDescending { it.rssi }
    }

    private var isScanning = false

    fun startScanning() {
        if (isScanning) return
        runCatching {
            beaconManager.addRangeNotifier(rangeNotifier)
            beaconManager.startRangingBeacons(region)
            isScanning = true
        }
    }

    fun stopScanning() {
        if (!isScanning) return
        runCatching {
            beaconManager.stopRangingBeacons(region)
            beaconManager.removeRangeNotifier(rangeNotifier)
        }
        isScanning = false
    }

    private fun Beacon.toDetectedBeacon(nowMs: Long): DetectedBeacon {
        val uuid = id1?.toString().orEmpty()
        val major = runCatching { id2?.toInt() ?: 0 }.getOrDefault(0)
        val minor = runCatching { id3?.toInt() ?: 0 }.getOrDefault(0)
        val mac = bluetoothAddress.orEmpty()
        val beaconId = if (uuid.isNotEmpty()) "$uuid:$major:$minor" else mac
        val displayName = bluetoothName?.takeIf { it.isNotBlank() }
            ?: if (major != 0 || minor != 0) "iBeacon #$major-$minor" else "Beacon ($mac)"

        return DetectedBeacon(
            id = beaconId,
            uuid = uuid.ifEmpty { mac },
            major = major,
            minor = minor,
            name = displayName,
            macAddress = mac,
            rssi = rssi,
            txPower = txPower,
            lastSeenMs = nowMs
        )
    }

    companion object {
        private const val IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    }
}
