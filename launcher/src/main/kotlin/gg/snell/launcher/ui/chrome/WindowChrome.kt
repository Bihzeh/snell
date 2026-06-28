package gg.snell.launcher.ui.chrome

import androidx.compose.runtime.Composable

/**
 * Window-level chrome hooks passed into the [gg.snell.launcher.ui.Shell] so the custom title
 * bar can drive the real OS window (drag / minimize / maximize / close) in the running app,
 * while the same composable tree stays renderable in the headless preview and tests — where
 * every hook is a no-op and [dragWrapper] is a pass-through.
 *
 * Why the indirection: `WindowDraggableArea` requires a `WindowScope` receiver that only exists
 * inside `Window { }`. Capturing it in [dragWrapper] keeps `Shell` free of window dependencies.
 */
class WindowChrome(
    val onMinimize: () -> Unit = {},
    val onToggleMaximize: () -> Unit = {},
    val onClose: () -> Unit = {},
    val dragWrapper: @Composable (@Composable () -> Unit) -> Unit = { it() },
) {
    companion object {
        /** No-op chrome for previews/tests (renders the title bar, no window behavior). */
        val Preview = WindowChrome()
    }
}
