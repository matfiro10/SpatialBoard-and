package com.example.spatialboard.permissions

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R
import com.example.spatialboard.ui.components.OverlayWindow
import com.example.spatialboard.ui.theme.capsuleNavigation.SpatialBoardTheme

@Composable
fun PermissionWindow(
    isPermanentlyDenied: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    OverlayWindow(
        modifier = modifier,
        title = stringResource(R.string.permissions_title)
    ) {
        Text(
            text = stringResource(R.string.permissions_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionItemRow(
                icon = Icons.Rounded.CameraAlt,
                titleResId = R.string.permissions_camera_title,
                descResId = R.string.permissions_camera_desc
            )
            PermissionItemRow(
                icon = Icons.Rounded.Bluetooth,
                titleResId = R.string.permissions_ble_title,
                descResId = R.string.permissions_ble_desc
            )
            PermissionItemRow(
                icon = Icons.Rounded.LocationOn,
                titleResId = R.string.permissions_location_title,
                descResId = R.string.permissions_location_desc
            )
        }

        Button(
            onClick = if (isPermanentlyDenied) onOpenSettings else onRequestPermissions,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape
        ) {
            Text(
                text = stringResource(
                    if (isPermanentlyDenied) {
                        R.string.permissions_action_settings
                    } else {
                        R.string.permissions_action_grant
                    }
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    @StringRes titleResId: Int,
    @StringRes descResId: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleResId),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(descResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionWindowPreview() {
    SpatialBoardTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            PermissionWindow(
                isPermanentlyDenied = false,
                onRequestPermissions = {},
                onOpenSettings = {}
            )
        }
    }
}
