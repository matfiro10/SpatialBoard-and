package com.example.spatialboard.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.spatialboard.R
import com.example.spatialboard.ui.theme.capsuleNavigation.SpatialBoardTheme

data class NavCapsuleButton(
    @StringRes val nameResId: Int,
    val icon: ImageVector
)

@Composable
fun NavigationCapsuleButton(
    button: NavCapsuleButton,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        label = "buttonContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "buttonContentColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = button.icon,
                contentDescription = stringResource(button.nameResId),
                tint = contentColor,
                modifier = Modifier.fillMaxSize(0.45f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NavigationCapsuleButtonPreview() {
    SpatialBoardTheme {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavigationCapsuleButton(
                button = NavCapsuleButton(R.string.nav_boards, Icons.Rounded.ViewCarousel),
                isSelected = false,
                onClick = {},
                modifier = Modifier.size(56.dp)
            )
            NavigationCapsuleButton(
                button = NavCapsuleButton(R.string.nav_add, Icons.Rounded.Add),
                isSelected = true,
                onClick = {},
                modifier = Modifier.size(56.dp)
            )
        }
    }
}
