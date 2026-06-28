package gg.snell.backend

import gg.snell.shared.CosmeticsLookupResponse
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Snell backend (Phase 3+). Stub: health check + an empty cosmetics lookup so the
 * contract from :shared is exercised end to end. Real identity, ownership, sync,
 * and friends/parties land in Phase 3-4. The backend NEVER stores Microsoft tokens
 * (ADR-0006); it keys its own identity to the Minecraft UUID.
 */
fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    routing {
        get("/health") { call.respondText("ok") }
        get("/v1/cosmetics") {
            // Phase 3: parse ?uuids=, look up equipped cosmetics, return them.
            // Unknown UUIDs are simply absent -> the client renders them vanilla.
            call.respond(CosmeticsLookupResponse(players = emptyList()))
        }
    }
}
