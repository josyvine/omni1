package com.vineyard.omnicam.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.vineyard.omnicam.app.data.models.CameraEntity

@Composable
fun CameraTileView(
    camera: CameraEntity,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
    onCameraClick: (CameraEntity) -> Unit,
    onPtzToggle: (() -> Unit)? = null,
    onFullscreenToggle: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable { onCameraClick(camera) }
            .testTag("camera_tile_${camera.id}")
    ) {
        VlcVideoPlayerSurface(
            camera = camera,
            isFocusMode = false,
            onPtzToggle = onPtzToggle,
            onFullscreenToggle = onFullscreenToggle
        )
    }
}
