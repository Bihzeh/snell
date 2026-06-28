package gg.snell.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource

/**
 * Player skin rendered as pre-baked yaw frames (NMSR 3D render), spun by horizontal drag — a
 * LabyMod-style "grab and rotate" quirk. Frames live at `skin/rot/NN.png` (0..[frameCount]-1),
 * pre-cropped at the knees.
 *
 * The frames are 360x464 hard-edged renders, so on a large launch card they get upscaled. We
 * decode each to an [ImageBitmap] (cached per frame, decoded lazily the first time it's shown
 * while rotating) and draw with [FilterQuality.None] — nearest-neighbour. The default bilinear
 * smears the crisp pixel edges into a soft/blurry mush as the upscale factor grows; nearest
 * keeps them sharp at any size. The real ceiling is re-baking the frames at a higher resolution
 * (the source was downscaled when baked); this just stops us blurring what's already there.
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
    // Decode-once-per-frame cache: rotating revisits frames repeatedly, so caching avoids
    // re-decoding the PNG on every drag step. 24 frames * 360x464 RGBA ~= 16 MB worst case.
    val frames = remember(frameCount) { arrayOfNulls<ImageBitmap>(frameCount) }
    val bitmap = frames[frame]
        ?: useResource("skin/rot/%02d.png".format(frame)) { loadImageBitmap(it) }.also { frames[frame] = it }
    Image(
        bitmap = bitmap,
        contentDescription = "Player skin — drag to rotate",
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
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
