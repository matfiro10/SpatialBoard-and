package com.example.spatialboard.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R
import com.example.spatialboard.ui.theme.capsuleNavigation.SpatialBoardTheme

val navCapsuleButtons = listOf(
    NavCapsuleButton(R.string.nav_boards, Icons.Rounded.ViewCarousel),
    NavCapsuleButton(R.string.nav_add, Icons.Rounded.Add),
    NavCapsuleButton(R.string.nav_beacons, Icons.Rounded.Radar),
    NavCapsuleButton(R.string.nav_settings, Icons.Rounded.Tune)
)

@Composable
fun NavigationCapsule(
    @StringRes selectedButtonResId: Int?,
    onButtonClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    buttons: List<NavCapsuleButton> = navCapsuleButtons
) {
    val layoutDirection = LocalLayoutDirection.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
    val minBottomInset = if (isLandscape) 12.dp else 24.dp
    val extraBottomMargin = if (isLandscape) 8.dp else 12.dp
    val bottomPadding = safeInsets.calculateBottomPadding().coerceAtLeast(minBottomInset) + extraBottomMargin

    val innerPadding = if (isLandscape) 6.dp else 8.dp
    val buttonSpacing = if (isLandscape) 6.dp else 8.dp

    val capsuleSizeModifier = if (isLandscape) {
        Modifier
            .fillMaxWidth(0.36f)
            .widthIn(min = 224.dp, max = 256.dp)
    } else {
        Modifier
            .fillMaxWidth(0.7f)
            .widthIn(min = 232.dp, max = 304.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = safeInsets.calculateStartPadding(layoutDirection),
                end = safeInsets.calculateEndPadding(layoutDirection),
                top = 8.dp,
                bottom = bottomPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = capsuleSizeModifier,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            ),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                buttons.forEach { button ->
                    NavigationCapsuleButton(
                        button = button,
                        isSelected = button.nameResId == selectedButtonResId,
                        onClick = { onButtonClick(button.nameResId) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 320, widthDp = 412, name = "Portrait")
@Preview(showBackground = true, heightDp = 360, widthDp = 800, name = "Landscape")
@Composable
private fun NavigationCapsulePreview() {
    var selectedResId by remember { mutableStateOf<Int?>(R.string.nav_boards) }

    SpatialBoardTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.BottomCenter
        ) {
            NavigationCapsule(
                selectedButtonResId = selectedResId,
                onButtonClick = { resId ->
                    selectedResId = if (selectedResId == resId) null else resId
                }
            )
        }
    }
}
