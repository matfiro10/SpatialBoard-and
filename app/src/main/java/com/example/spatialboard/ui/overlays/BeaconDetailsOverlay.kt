package com.example.spatialboard.ui.overlays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R
import com.example.spatialboard.model.BeaconItem
import com.example.spatialboard.ui.components.rememberOverlayContentMaxHeight

@Composable
fun BeaconDetailsOverlayContent(
    beacon: BeaconItem,
    modifier: Modifier = Modifier,
    onCreateBoard: ((String) -> Unit)? = null
) {
    val maxContentHeight = rememberOverlayContentMaxHeight()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxContentHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "beacon_status") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = beacon.zoneLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = CircleShape,
                    color = if (beacon.isInRange) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = stringResource(
                            if (beacon.isInRange) {
                                R.string.beacon_status_active
                            } else {
                                R.string.beacon_status_out_of_range
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (beacon.isInRange) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        item(key = "beacon_metrics_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BeaconMetricCard(
                        icon = Icons.Rounded.NearMe,
                        label = stringResource(R.string.beacon_metric_distance),
                        value = stringResource(R.string.beacons_distance_format, beacon.distanceMeters),
                        modifier = Modifier.weight(1f)
                    )
                    BeaconMetricCard(
                        icon = Icons.Rounded.BluetoothConnected,
                        label = stringResource(R.string.beacon_metric_rssi),
                        value = stringResource(R.string.beacon_rssi_format, beacon.rssiDbm),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BeaconMetricCard(
                        icon = Icons.Rounded.Tag,
                        label = stringResource(R.string.beacon_metric_major_minor),
                        value = stringResource(R.string.beacon_major_minor_format, beacon.major, beacon.minor),
                        modifier = Modifier.weight(1f)
                    )
                    BeaconMetricCard(
                        icon = Icons.Rounded.Dashboard,
                        label = stringResource(R.string.beacon_metric_boards),
                        value = beacon.linkedBoardsCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item(key = "beacon_technical_info") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.beacon_detail_uuid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = beacon.uuid,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    BeaconSpecRow(
                        label = stringResource(R.string.beacon_detail_mac),
                        value = beacon.macAddress
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    BeaconSpecRow(
                        label = stringResource(R.string.beacon_detail_tx_power),
                        value = stringResource(R.string.beacon_rssi_format, beacon.txPowerDbm)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    BeaconSpecRow(
                        label = stringResource(R.string.beacon_detail_last_seen),
                        value = beacon.lastSeenLabel
                    )
                }
            }
        }

        if (onCreateBoard != null) {
            item(key = "beacon_create_board_action") {
                FilledTonalButton(
                    onClick = { onCreateBoard(beacon.id) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.beacon_create_board_action))
                }
            }
        }
    }
}

@Composable
private fun BeaconMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BeaconSpecRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
