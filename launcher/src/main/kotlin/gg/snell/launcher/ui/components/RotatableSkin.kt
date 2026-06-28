package gg.snell.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Player skin rendered as pre-baked yaw frames (NMSR 3D render), spun by horizontal drag — a
 * LabyMod-style "grab and rotate" quirk. Frames live at `skin/rot/NN.png` (0..[frameCount]-1),
 * pre-cropped at the knees. Only the current frame is decoded, so memory stays flat.
 *
 * Frames are baked for the reference skin for now; this swaps to a runtime per-user fetch (by the
 * signed-in UUID) once real Microsoft auth lands.
 */
@Composable
fun RotatableSkin(
    frameCount: Int = 24,
    modifier: Modifier = Modifier,
    pxPerFrame: Float = 16f,
) {
    var frame by remember { mutableStateOf(0) }
    var acc by remember { mutableStateOf(0f) }
    Image(
        painter = painterResource("skin/rot/%02d.png".format(frame)),
        contentDescription = "Player skin — drag to rotate",
        contentScale = ContentScale.Fit,
        modifier = modifier.pointerInput(frameCount) {
            detectHorizontalDragGestures { change, dragAmount ->
                change.consume()
                acc += dragAmount
                while (acc >= pxPerFrame) { frame = (frame + 1) % frameCount; acc -= pxPerFrame }
                while (acc <= -pxPerFrame) { frame = (frame - 1 + frameCount) % frameCount; acc += pxPerFrame }
            }
        },
    )
}
