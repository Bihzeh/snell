package gg.snell.launcher.auth

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthConfigTest {
    @Test fun `default client id is a non-blank GUID`() {
        assertTrue(AuthConfig.DEFAULT_CLIENT_ID.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")))
    }

    @Test fun `falls back to the baked-in default when no env or file`() {
        if (System.getenv(AuthConfig.ENV) != null) return // a machine-level override is present; skip
        assertEquals(AuthConfig.DEFAULT_CLIENT_ID, AuthConfig.clientId(Files.createTempDirectory("auth")))
    }

    @Test fun `azure_client_id file overrides the default`() {
        val dir = Files.createTempDirectory("auth")
        Files.writeString(dir.resolve("azure_client_id.txt"), "  file-override-id  ")
        assertEquals("file-override-id", AuthConfig.clientId(dir))
    }
}
