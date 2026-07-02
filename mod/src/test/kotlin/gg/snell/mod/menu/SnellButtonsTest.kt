package gg.snell.mod.menu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pure-layout regression for the Snell buttons injected into the vanilla title/pause screens. */
class SnellButtonsTest {
    // Stub font metric: 6px per char (deterministic, no Minecraft).
    private val widthOf: (String) -> Int = { it.length * 6 }

    @Test fun `cluster carries all three actions with stable labels`() {
        val specs = SnellButtons.cluster(1920, 1080, widthOf)
        assertEquals(
            listOf(SnellButtonAction.HudEditor, SnellButtonAction.Discord, SnellButtonAction.Cosmetics),
            specs.map { it.action },
            "all three actions, in order",
        )
        assertTrue(specs.all { it.label.isNotBlank() }, "every button has a label")
    }

    @Test fun `buttons sit inside the screen, right-aligned near the top`() {
        val w = 1920; val h = 1080
        val specs = SnellButtons.cluster(w, h, widthOf)
        specs.forEach {
            assertTrue(it.rect.left >= 0 && it.rect.right <= w, "in horizontal bounds: ${it.action} ${it.rect}")
            assertTrue(it.rect.top >= 0 && it.rect.bottom <= h, "in vertical bounds: ${it.action} ${it.rect}")
            assertTrue(it.rect.top < h / 4, "near the top: ${it.action} ${it.rect}")
        }
        assertTrue(specs.last().rect.right >= w - 10, "row is right-aligned")
    }

    @Test fun `buttons flow left to right without overlapping`() {
        val specs = SnellButtons.cluster(1280, 720, widthOf)
        for (i in 1 until specs.size) {
            assertTrue(specs[i].rect.left >= specs[i - 1].rect.right, "no overlap between ${specs[i - 1].action} and ${specs[i].action}")
        }
    }

    @Test fun `button width follows its label`() {
        val specs = SnellButtons.cluster(1280, 720, widthOf)
        val editor = specs.first { it.action == SnellButtonAction.HudEditor }
        val discord = specs.first { it.action == SnellButtonAction.Discord }
        assertTrue(editor.rect.width > discord.rect.width, "the longer 'HUD Editor' label is wider than 'Discord'")
    }
}
