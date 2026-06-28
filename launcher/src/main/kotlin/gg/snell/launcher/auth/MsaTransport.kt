package gg.snell.launcher.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.parameters

/** One HTTP exchange: status + body (auth flows must read 4xx bodies, not throw). */
data class HttpResult(val status: Int, val body: String) {
    val ok: Boolean get() = status in 200..299
}

/** The HTTP surface the auth flow needs; injectable so the flow is testable. */
interface MsaTransport {
    suspend fun postForm(url: String, form: Map<String, String>): HttpResult
    suspend fun postJson(url: String, jsonBody: String): HttpResult
    suspend fun getBearer(url: String, bearer: String): HttpResult
}

/** Ktor-backed transport. Default client does not throw on non-2xx (expectSuccess=false). */
class KtorMsaTransport(private val client: HttpClient = HttpClient(CIO)) : MsaTransport, AutoCloseable {
    override suspend fun postForm(url: String, form: Map<String, String>): HttpResult {
        val resp = client.submitForm(url, parameters { form.forEach { (k, v) -> append(k, v) } })
        return HttpResult(resp.status.value, resp.bodyAsText())
    }

    override suspend fun postJson(url: String, jsonBody: String): HttpResult {
        val resp = client.post(url) { contentType(ContentType.Application.Json); setBody(jsonBody) }
        return HttpResult(resp.status.value, resp.bodyAsText())
    }

    override suspend fun getBearer(url: String, bearer: String): HttpResult {
        val resp = client.get(url) { header(HttpHeaders.Authorization, "Bearer $bearer") }
        return HttpResult(resp.status.value, resp.bodyAsText())
    }

    override fun close() = client.close()
}
