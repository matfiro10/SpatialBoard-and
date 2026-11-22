package com.example.spatialboard.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

class SpatialSettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _minRssiThreshold = MutableStateFlow(
        prefs.getInt(KEY_MIN_RSSI, DEFAULT_MIN_RSSI).coerceIn(MIN_RSSI_LIMIT, MAX_RSSI_LIMIT)
    )
    val minRssiThreshold: StateFlow<Int> = _minRssiThreshold.asStateFlow()

    fun setMinRssiThreshold(rssi: Int) {
        val clamped = rssi.coerceIn(MIN_RSSI_LIMIT, MAX_RSSI_LIMIT)
        if (_minRssiThreshold.value == clamped) return
        prefs.edit { putInt(KEY_MIN_RSSI, clamped) }
        _minRssiThreshold.value = clamped
    }

    companion object {
        private const val PREFS_NAME = "spatial_board_settings"
        private const val KEY_MIN_RSSI = "ble_min_rssi_threshold"
        const val MIN_RSSI_LIMIT = -95
        const val MAX_RSSI_LIMIT = -50
        const val DEFAULT_MIN_RSSI = -75
    }
}
