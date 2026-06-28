package gg.snell.mod.editor

import kotlin.math.roundToInt

/** Pure mapping between a slider value range and a pixel length. No Minecraft types. */
object SliderMath {
    fun valueToCoord(value: Float, min: Float, max: Float, len: Int): Int =
        (((value - min) / (max - min)).coerceIn(0f, 1f) * len).roundToInt()

    fun coordToValue(coord: Int, len: Int, min: Float, max: Float): Float =
        min + (coord.toFloat() / len).coerceIn(0f, 1f) * (max - min)

    /** For vertical sliders where the top of the track is the maximum value. */
    fun coordToValueInverted(coord: Int, len: Int, min: Float, max: Float): Float =
        min + (1f - (coord.toFloat() / len).coerceIn(0f, 1f)) * (max - min)
}
