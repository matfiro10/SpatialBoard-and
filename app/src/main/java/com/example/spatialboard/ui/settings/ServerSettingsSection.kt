package com.example.spatialboard.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R
import com.example.spatialboard.data.DataSourceMode
import com.example.spatialboard.data.SpatialSettingsStore
import com.example.spatialboard.data.remote.ServerSyncState

@Composable
fun ServerSettingsSection(
    dataSourceMode: DataSourceMode,
    serverHost: String,
    serverPort: Int,
    syncState: ServerSyncState,
    onModeChange: (DataSourceMode) -> Unit,
    onSaveEndpoint: (String, Int) -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draftHost by rememberSaveable(serverHost) { mutableStateOf(serverHost) }
    var draftPort by rememberSaveable(serverPort) { mutableStateOf(serverPort.toString()) }

    val parsedPort = draftPort.toIntOrNull()
    val isPortValid = parsedPort != null &&
            parsedPort in SpatialSettingsStore.MIN_PORT..SpatialSettingsStore.MAX_PORT
    val isHostValid = draftHost.isNotBlank()
    val hasUnsavedChanges = draftHost.trim() != serverHost || parsedPort != serverPort

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.settings_source_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = if (dataSourceMode == DataSourceMode.SERVER) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = stringResource(
                            if (dataSourceMode == DataSourceMode.SERVER) {
                                R.string.settings_source_badge_server
                            } else {
                                R.string.settings_source_badge_local
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dataSourceMode == DataSourceMode.SERVER) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = dataSourceMode == DataSourceMode.LOCAL,
                    onClick = { onModeChange(DataSourceMode.LOCAL) },
                    label = { Text(stringResource(R.string.settings_source_local)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = dataSourceMode == DataSourceMode.SERVER,
                    onClick = { onModeChange(DataSourceMode.SERVER) },
                    label = { Text(stringResource(R.string.settings_source_server)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = stringResource(
                    if (dataSourceMode == DataSourceMode.SERVER) {
                        R.string.settings_source_desc_server
                    } else {
                        R.string.settings_source_desc_local
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draftHost,
                    onValueChange = { input ->
                        val cleaned = input.trim()
                            .removePrefix("http://")
                            .removePrefix("https://")
                            .substringBefore("/")

                        if (":" in cleaned) {
                            draftHost = cleaned.substringBefore(":")
                            val extractedPort = cleaned.substringAfter(":")
                                .filter { it.isDigit() }
                                .take(5)
                            if (extractedPort.isNotEmpty()) {
                                draftPort = extractedPort
                            }
                        } else {
                            draftHost = cleaned
                        }
                    },
                    label = { Text(stringResource(R.string.settings_server_ip_label)) },
                    placeholder = { Text(SpatialSettingsStore.DEFAULT_SERVER_HOST) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(0.68f)
                )

                OutlinedTextField(
                    value = draftPort,
                    onValueChange = { input ->
                        draftPort = input.filter { it.isDigit() }.take(5)
                    },
                    label = { Text(stringResource(R.string.settings_server_port_label)) },
                    placeholder = { Text(SpatialSettingsStore.DEFAULT_SERVER_PORT.toString()) },
                    singleLine = true,
                    isError = draftPort.isNotEmpty() && !isPortValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(0.32f)
                )
            }

            if (syncState !is ServerSyncState.Idle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (syncState) {
                        ServerSyncState.Syncing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.settings_server_syncing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is ServerSyncState.Connected -> {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            val statusText = syncState.serverMessage
                                ?: if (syncState.formatArg != null) {
                                    stringResource(syncState.messageResId, syncState.formatArg)
                                } else {
                                    stringResource(syncState.messageResId)
                                }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is ServerSyncState.Error -> {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            val baseError = stringResource(syncState.messageResId)
                            val errorText = if (!syncState.detail.isNullOrBlank()) {
                                "$baseError: ${syncState.detail}"
                            } else {
                                baseError
                            }
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        ServerSyncState.Idle -> Unit
                    }
                }
            }

            Button(
                onClick = {
                    val port = parsedPort ?: SpatialSettingsStore.DEFAULT_SERVER_PORT
                    onSaveEndpoint(draftHost, port)
                },
                enabled = isHostValid && isPortValid && hasUnsavedChanges,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (hasUnsavedChanges) Icons.Rounded.Sync else Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (hasUnsavedChanges) {
                            R.string.settings_server_save
                        } else {
                            R.string.settings_server_saved
                        }
                    )
                )
            }

            OutlinedButton(
                onClick = {
                    if (isHostValid && isPortValid && hasUnsavedChanges) {
                        onSaveEndpoint(draftHost, parsedPort)
                    }
                    onTestConnection()
                },
                enabled = isHostValid && isPortValid && syncState !is ServerSyncState.Syncing,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.settings_server_test_connection))
            }
        }
    }
}
