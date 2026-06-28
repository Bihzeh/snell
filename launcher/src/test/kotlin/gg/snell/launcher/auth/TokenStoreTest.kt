package gg.snell.launcher.auth

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenStoreTest {
    @Test fun `refresh token round-trips encrypted`() {
        val dir = Files.createTempDirectory("snell-tok")
        val store = FileTokenStore(dir)
        store.saveRefreshToken("default", "secret-refresh-token")

        // Persisted form is not plaintext.
        val raw = Files.readAllBytes(dir.resolve("token-default.enc")).toString(Charsets.UTF_8)
        assert(!raw.contains("secret-refresh-token"))

        assertEquals("secret-refresh-token", FileTokenStore(dir).loadRefreshToken("default"))
        store.clear("default")
        assertNull(store.loadRefreshToken("default"))
    }
}
