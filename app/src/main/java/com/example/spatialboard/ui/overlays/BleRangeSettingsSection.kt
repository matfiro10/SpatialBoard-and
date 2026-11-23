package com.example.spatialboard.ui.overlays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R
import com.example.spatialboard.ble.BleRangePreset
import com.example.spatialboard.data.SpatialSettingsStore
import kotlin.math.roundToInt

@Composable
fun BleRangeSettingsSection(
    minRssiThreshold: Int,
    onThresholdChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val preset = BleRangePreset.fromRssi(minRssiThreshold)
    val presetDescription = when (preset) {
        BleRangePreset.IMMEDIATE -> stringResource(R.string.settings_ble_preset_immediate)
        BleRangePreset.NEAR -> stringResource(R.string.settings_ble_preset_near)
        BleRangePreset.ROOM -> stringResource(R.string.settings_ble_preset_room)
        BleRangePreset.WIDE -> stringResource(R.string.settings_ble_preset_wide)
    }

    val sliderState = rememberSliderState(
        value = minRssiThreshold.toFloat(),
        trackRange = SpatialSettingsStore.MIN_RSSI_LIMIT.toFloat()..SpatialSettingsStore.MAX_RSSI_LIMIT.toFloat(),
        steps = 8
    )

    LaunchedEffect(minRssiThreshold) {
        if (sliderState.value.roundToInt() != minRssiThreshold) {
            sliderState.value = minRssiThreshold.toFloat()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.settings_ble_range_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = stringResource(
                            R.string.settings_ble_range_badge,
                            preset.approxMeters,
                            minRssiThreshold
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = presetDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                state = sliderState,
                onValueChange = { raw ->
                    val snapped = (raw / 5f).roundToInt() * 5
                    sliderState.value = snapped.toFloat()
                    if (snapped != minRssiThreshold) {
                        onThresholdChange(snapped)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_ble_scale_wide),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.settings_ble_scale_near),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
