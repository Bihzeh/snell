package gg.maeve.launcher.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemVerTest {
    @Test fun `parses with and without v prefix`() {
        assertEquals(SemVer(0, 1, 3), SemVer.parse("v0.1.3"))
        assertEquals(SemVer(1, 2, 0), SemVer.parse("1.2"))
        assertNull(SemVer.parse("nope"))
    }
    @Test fun `orders correctly`() {
        assertTrue(SemVer.parse("0.1.4")!! > SemVer.parse("0.1.3")!!)
        assertTrue(SemVer.parse("0.2.0")!! > SemVer.parse("0.1.9")!!)
        assertTrue(SemVer.parse("1.0.0")!! > SemVer.parse("0.9.9")!!)
    }
}

class UpdateServiceTest {
    private val svc = UpdateService(current = null, fetchReleases = { "" })

    private fun release(tag: String, vararg assets: String) = GhRelease(
        tagName = tag,
        assets = assets.map { GhAsset(it, "https://dl/$it", 1) } + GhAsset("SHA256SUMS.txt", "https://dl/sums", 1),
    )

    @Test fun `picks newest release with an installer, newer than current`() {
        val releases = listOf(
            release("v0.1.4", "Maeve-0.1.4.exe", "Maeve-0.1.4.msi"),
            release("v0.1.3", "Maeve-0.1.3.exe"),
        )
        val info = svc.selectUpdate(releases, SemVer.parse("0.1.3"))!!
        assertEquals("v0.1.4", info.tag)
        assertTrue(info.installerName.endsWith(".exe"), info.installerName)
        assertEquals("https://dl/sums", info.sha256SumsUrl)
    }

    @Test fun `returns null when already up to date`() {
        val releases = listOf(release("v0.1.3", "Maeve-0.1.3.exe"))
        assertNull(svc.selectUpdate(releases, SemVer.parse("0.1.3")))
    }

    @Test fun `skips releases without an installer asset`() {
        val r = GhRelease(tagName = "v0.2.0", assets = listOf(GhAsset("notes.txt", "https://dl/n", 1)))
        assertNull(svc.selectUpdate(listOf(r), SemVer.parse("0.1.3")))
    }

    @Test fun `extracts sha256 for the right file`() {
        val sums = "abc  other.exe\n" + "b".repeat(64) + "  Maeve-0.1.4.exe\n"
        assertEquals("b".repeat(64), UpdateService.sha256For(sums, "Maeve-0.1.4.exe"))
    }
}
