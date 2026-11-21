package com.example.spatialboard

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.spatialboard.camera.ArCameraView
import com.example.spatialboard.permissions.AppPermissions
import com.example.spatialboard.permissions.PermissionWindow
import com.example.spatialboard.ui.ActiveOverlay
import com.example.spatialboard.ui.NavigationCapsule
import com.example.spatialboard.ui.OverlayWindow
import com.example.spatialboard.ui.theme.capsuleNavigation.SpatialBoardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContent {
            SpatialBoardTheme {
                SpatialBoardTestScreen()
            }
        }
    }
}

@Composable
fun SpatialBoardTestScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTabResId by rememberSaveable { mutableStateOf<Int?>(null) }
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

    val activeOverlay = selectedTabResId?.let { ActiveOverlay.NavigationCard(it) }
        ?: if (!hasAllPermissions && hasRequestedOnce) ActiveOverlay.Permissions else ActiveOverlay.None

    val isDismissible = activeOverlay is ActiveOverlay.NavigationCard

    BackHandler(enabled = isDismissible) {
        selectedTabResId = null
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
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = isDismissible
                ) {
                    selectedTabResId = null
                }
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = scrimModifier)

        AnimatedContent(
            targetState = activeOverlay,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.94f))
                    .togetherWith(fadeOut() + scaleOut(targetScale = 0.94f))
            },
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .safeDrawingPadding()
                .padding(24.dp),
            label = "overlayContent"
        ) { overlay ->
            when (overlay) {
                ActiveOverlay.None -> Unit
                ActiveOverlay.Permissions -> {
                    PermissionWindow(
                        isPermanentlyDenied = isPermanentlyDenied,
                        onRequestPermissions = {
                            permissionLauncher.launch(AppPermissions.requiredPermissions)
                        },
                        onOpenSettings = {
                            AppPermissions.openAppSettings(context)
                        }
                    )
                }
                is ActiveOverlay.NavigationCard -> {
                    OverlayWindow {
                        Text(
                            text = stringResource(overlay.titleResId),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
                        )
                    }
                }
            }
        }

        NavigationCapsule(
            selectedButtonResId = selectedTabResId,
            onButtonClick = { resId ->
                selectedTabResId = if (selectedTabResId == resId) null else resId
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SpatialBoardTestScreenPreview() {
    SpatialBoardTheme {
        SpatialBoardTestScreen()
    }
}
