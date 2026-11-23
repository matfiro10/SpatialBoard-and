package com.example.spatialboard.data

import android.content.Context
import com.example.spatialboard.ui.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.core.content.edit

enum class DataSourceMode {
    LOCAL,
    SERVER
}

class SpatialSettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _minRssiThreshold = MutableStateFlow(
        prefs.getInt(KEY_MIN_RSSI, DEFAULT_MIN_RSSI)
            .coerceIn(MIN_RSSI_LIMIT, MAX_RSSI_LIMIT)
    )
    val minRssiThreshold: StateFlow<Int> = _minRssiThreshold.asStateFlow()

    private val _serverConfig = MutableStateFlow(
        ServerConfig(
            host = prefs.getString(KEY_SERVER_HOST, DEFAULT_SERVER_HOST).orEmpty().ifBlank { DEFAULT_SERVER_HOST },
            port = prefs.getInt(KEY_SERVER_PORT, DEFAULT_SERVER_PORT).coerceIn(MIN_PORT, MAX_PORT),
            mode = prefs.getString(KEY_DATA_SOURCE_MODE, null)
                ?.let { raw -> runCatching { DataSourceMode.valueOf(raw) }.getOrNull() }
                ?: DataSourceMode.LOCAL
        )
    )
    val serverConfig: StateFlow<ServerConfig> = _serverConfig.asStateFlow()

    fun setMinRssiThreshold(rssi: Int) {
        val clamped = rssi.coerceIn(MIN_RSSI_LIMIT, MAX_RSSI_LIMIT)
        if (_minRssiThreshold.value == clamped) return
        _minRssiThreshold.value = clamped
        prefs.edit { putInt(KEY_MIN_RSSI, clamped) }
    }

    fun setDataSourceMode(mode: DataSourceMode) {
        if (_serverConfig.value.mode == mode) return
        _serverConfig.update { it.copy(mode = mode) }
        prefs.edit { putString(KEY_DATA_SOURCE_MODE, mode.name) }
    }

    fun setServerEndpoint(host: String, port: Int) {
        val normalizedHost = host.trim().ifBlank { DEFAULT_SERVER_HOST }
        val clampedPort = port.coerceIn(MIN_PORT, MAX_PORT)
        _serverConfig.update { it.copy(host = normalizedHost, port = clampedPort) }
        prefs.edit {
            putString(KEY_SERVER_HOST, normalizedHost)
                .putInt(KEY_SERVER_PORT, clampedPort)
        }
    }

    companion object {
        private const val PREFS_NAME = "spatial_board_settings"
        private const val KEY_MIN_RSSI = "min_rssi_threshold"
        private const val KEY_DATA_SOURCE_MODE = "data_source_mode"
        private const val KEY_SERVER_HOST = "server_host"
        private const val KEY_SERVER_PORT = "server_port"

        const val DEFAULT_MIN_RSSI = -80
        const val MIN_RSSI_LIMIT = -95
        const val MAX_RSSI_LIMIT = -50

        const val DEFAULT_SERVER_HOST = "192.168.1.100"
        const val DEFAULT_SERVER_PORT = 3000
        const val MIN_PORT = 1
        const val MAX_PORT = 65535
    }
}
