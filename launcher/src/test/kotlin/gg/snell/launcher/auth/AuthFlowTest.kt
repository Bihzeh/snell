package gg.snell.launcher.auth

import gg.snell.launcher.auth.MsaDeviceCodeAuth.Companion.DEVICE_CODE_URL
import gg.snell.launcher.auth.MsaDeviceCodeAuth.Companion.MC_LOGIN_URL
import gg.snell.launcher.auth.MsaDeviceCodeAuth.Companion.MC_PROFILE_URL
import gg.snell.launcher.auth.MsaDeviceCodeAuth.Companion.TOKEN_URL
import gg.snell.launcher.auth.MsaDeviceCodeAuth.Companion.XBL_URL
import gg.snell.launcher.auth.MsaDeviceCodeAuth.Companion.XSTS_URL
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Canned-response transport keyed by URL; each URL yields its queued results in order. */
private class FakeTransport(private val q: Map<String, ArrayDeque<HttpResult>>) : MsaTransport {
    override suspend fun postForm(url: String, form: Map<String, String>) = next(url)
    override suspend fun postJson(url: String, jsonBody: String) = next(url)
    override suspend fun getBearer(url: String, bearer: String) = next(url)
    private fun next(url: String) = q[url]?.removeFirstOrNull() ?: error("no canned response for $url")
}

private fun q(vararg r: HttpResult) = ArrayDeque(r.toList())
private fun ok(body: String) = HttpResult(200, body)

class AuthFlowTest {
    private val deviceCode = ok("""{"device_code":"DC","user_code":"ABCD-EFGH","verification_uri":"https://microsoft.com/link","expires_in":900,"interval":1}""")
    private val xbl = ok("""{"Token":"XBL","DisplayClaims":{"xui":[{"uhs":"UHS"}]}}""")
    private val xsts = ok("""{"Token":"XSTS","DisplayClaims":{"xui":[{"uhs":"UHS"}]}}""")
    private val mc = ok("""{"access_token":"MCTOKEN","expires_in":86400}""")
    private val profile = ok("""{"id":"00000000000000000000000000000001","name":"Steve"}""")

    private fun auth(transport: FakeTransport) = MsaDeviceCodeAuth("client", transport, sleep = {})

    @Test fun `device-code happy path yields msa session + refresh token`() = runBlocking {
        val t = FakeTransport(
            mapOf(
                DEVICE_CODE_URL to q(deviceCode),
                TOKEN_URL to q(
                    HttpResult(400, """{"error":"authorization_pending"}"""),
                    ok("""{"access_token":"MSACCESS","refresh_token":"REFRESH","expires_in":3600}"""),
                ),
                XBL_URL to q(xbl), XSTS_URL to q(xsts), MC_LOGIN_URL to q(mc), MC_PROFILE_URL to q(profile),
            ),
        )
        var shownCode: String? = null
        val result = auth(t).signInWithDeviceCode { shownCode = it.userCode }

        assertEquals("ABCD-EFGH", shownCode)
        assertEquals("00000000000000000000000000000001", result.session.uuid)
        assertEquals("Steve", result.session.username)
        assertEquals("MCTOKEN", result.session.accessToken)
        assertEquals("msa", result.session.userType)
        assertEquals("REFRESH", result.refreshToken)
    }

    @Test fun `XSTS no-xbox-account maps to a friendly error`() {
        val t = FakeTransport(
            mapOf(
                DEVICE_CODE_URL to q(deviceCode),
                TOKEN_URL to q(ok("""{"access_token":"MSACCESS","refresh_token":"R"}""")),
                XBL_URL to q(xbl),
                XSTS_URL to q(HttpResult(401, """{"XErr":2148916233,"Message":""}""")),
            ),
        )
        val ex = assertFailsWith<AuthException> { runBlocking { auth(t).signInWithDeviceCode { } } }
        assertTrue(ex.message!!.contains("Xbox profile"), ex.message!!)
    }

    @Test fun `no game ownership maps to a friendly error`() {
        val t = FakeTransport(
            mapOf(
                DEVICE_CODE_URL to q(deviceCode),
                TOKEN_URL to q(ok("""{"access_token":"MSACCESS"}""")),
                XBL_URL to q(xbl), XSTS_URL to q(xsts), MC_LOGIN_URL to q(mc),
                MC_PROFILE_URL to q(HttpResult(404, "")),
            ),
        )
        val ex = assertFailsWith<AuthException> { runBlocking { auth(t).signInWithDeviceCode { } } }
        assertTrue(ex.message!!.contains("doesn't own"), ex.message!!)
    }

    @Test fun `refresh-token path skips device code`() = runBlocking {
        val t = FakeTransport(
            mapOf(
                TOKEN_URL to q(ok("""{"access_token":"MSACCESS","refresh_token":"NEWREFRESH"}""")),
                XBL_URL to q(xbl), XSTS_URL to q(xsts), MC_LOGIN_URL to q(mc), MC_PROFILE_URL to q(profile),
            ),
        )
        val result = auth(t).signInWithRefreshToken("OLD")
        assertEquals("Steve", result.session.username)
        assertEquals("NEWREFRESH", result.refreshToken)
    }
}
