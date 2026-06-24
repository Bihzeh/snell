package gg.maeve.launcher.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class LibraryResolverTest {
    private fun lib(name: String, path: String, rules: List<Rule>? = null) = Library(
        name = name,
        downloads = Library.LibDownloads(Library.Artifact(path, "https://example/$path", "sha", 1)),
        rules = rules,
    )

    private val win = Platform("windows", "x64")
    private val linux = Platform("linux", "x64")

    @Test fun `plain lib always included`() {
        val r = LibraryResolver.resolve(listOf(lib("g:a:1", "g/a/1/a-1.jar")), linux)
        assertEquals(1, r.size)
        assertFalse(r[0].isNative)
    }

    @Test fun `os-gated lib respects rules`() {
        val libs = listOf(lib("g:a:1", "g/a/1/a-1.jar", listOf(Rule("allow", Rule.Os(name = "windows")))))
        assertEquals(1, LibraryResolver.resolve(libs, win).size)
        assertEquals(0, LibraryResolver.resolve(libs, linux).size)
    }

    @Test fun `natives filtered to current platform`() {
        val libs = listOf(
            lib("org.lwjgl:lwjgl:3:natives-windows", "n/win.jar", listOf(Rule("allow", Rule.Os(name = "windows")))),
            lib("org.lwjgl:lwjgl:3:natives-linux", "n/linux.jar", listOf(Rule("allow", Rule.Os(name = "linux")))),
        )
        val r = LibraryResolver.resolve(libs, linux)
        assertEquals(1, r.size)
        assertEquals("n/linux.jar", r[0].path)
        assertTrue(r[0].isNative)
    }

    @Test fun `dedupes by path`() {
        val libs = listOf(lib("g:a:1", "same.jar"), lib("g:a:1", "same.jar"))
        assertEquals(1, LibraryResolver.resolve(libs, linux).size)
    }
}
