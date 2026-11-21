package com.example.spatialboard.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.spatialboard.camera.ArCameraView
import com.example.spatialboard.permissions.AppPermissions
import com.example.spatialboard.ui.overlays.ActiveOverlay
import com.example.spatialboard.ui.overlays.OverlayHost
import com.example.spatialboard.ui.theme.capsuleNavigation.SpatialBoardTheme

@Composable
fun SpatialBoardScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrimInteractionSource = remember { MutableInteractionSource() }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var overlayStack by rememberSaveable(stateSaver = ActiveOverlay.StackSaver) {
        mutableStateOf<List<ActiveOverlay>>(emptyList())
    }

    var hasCameraPermission by rememberSaveable {
        mutableStateOf(AppPermissions.hasCameraPermission(context))
    }
    var hasAllPermissions by rememberSaveable {
        mutableStateOf(AppPermissions.hasAllPermissions(context))
    }
    var hasRequestedOnce by rememberSaveable { mutableStateOf(false) }
    var isPermanentlyDenied by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasCameraPermission = AppPermissions.hasCameraPermission(context)
        hasAllPermissions = AppPermissions.hasAllPermissions(context)
        hasRequestedOnce = true
        isPermanentlyDenied = !hasAllPermissions && AppPermissions.isPermanentlyDenied(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = AppPermissions.hasCameraPermission(context)
                hasAllPermissions = AppPermissions.hasAllPermissions(context)
                if (hasAllPermissions) {
                    isPermanentlyDenied = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAllPermissions && !hasRequestedOnce) {
            permissionLauncher.launch(AppPermissions.requiredPermissions)
        }
    }

    val activeOverlay = overlayStack.lastOrNull()
        ?: if (!hasAllPermissions && hasRequestedOnce) ActiveOverlay.Permissions else ActiveOverlay.None

    val isDismissible = overlayStack.isNotEmpty()

    BackHandler(enabled = isDismissible) {
        overlayStack = overlayStack.dropLast(1)
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (activeOverlay != ActiveOverlay.None) 0.86f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "scrimAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        if (hasCameraPermission) {
            ArCameraView(
                modifier = Modifier.fillMaxSize(),
                planeRenderer = false
            )
        }

        val scrimModifier = if (activeOverlay != ActiveOverlay.None) {
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = scrimInteractionSource,
                    indication = null
                ) {
                    if (isDismissible) {
                        overlayStack = emptyList()
                    }
                }
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = scrimModifier)

        OverlayHost(
            activeOverlay = activeOverlay,
            isPermanentlyDenied = isPermanentlyDenied,
            onRequestPermissions = {
                permissionLauncher.launch(AppPermissions.requiredPermissions)
            },
            onOpenSettings = {
                AppPermissions.openAppSettings(context)
            },
            onNavigate = { overlay ->
                overlayStack = overlayStack + overlay
            },
            onBack = {
                overlayStack = overlayStack.dropLast(1)
            },
            onCloseAll = {
                overlayStack = emptyList()
            },
            modifier = Modifier
                .align(Alignment.Center)
                .safeDrawingPadding()
                .imePadding()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = if (isLandscape) 12.dp else 24.dp,
                    bottom = if (isLandscape) 84.dp else 96.dp
                )
        )

        NavigationCapsule(
            selectedButtonResId = activeOverlay.parentTabResId,
            onButtonClick = { resId ->
                overlayStack = if (activeOverlay.parentTabResId == resId) {
                    emptyList()
                } else {
                    listOf(ActiveOverlay.NavigationCard(resId))
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SpatialBoardScreenPreview() {
    SpatialBoardTheme {
        SpatialBoardScreen()
    }
}
