package io.shubham0204.smollm_server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.shubham0204.smollm_server.models.OpenAIChatCompletionRequest
import kotlinx.serialization.json.Json

class LlamaServer {
    private val server =
        embeddedServer(
            factory = Netty,
            configure = {
                connector {
                    port = 8080
                }
            },
        ) {
            setupPlugins()
            setupRoutes()
        }

    fun start() {
        server.start(wait = false)
    }

    fun stop() {
        server.stop()
    }

    private fun Application.setupPlugins() {
        // install content-negotiation and
        // configure JSON serializer
        // docs: https://ktor.io/docs/server-serialization.html#install_plugin
        //       https://ktor.io/docs/server-serialization.html#register_xml
        install(ContentNegotiation) {
            json(
                Json {
                    isLenient = true
                    prettyPrint = true
                },
            )
        }
    }

    private fun Application.setupRoutes() {
        routing {
            get {
                call.respond(HttpStatusCode.OK, "Hello, world!")
            }
            route("v1") {
                route("chat") {
                    post("completions") {
                        val request = call.receive<OpenAIChatCompletionRequest>()
                    }
                }
                get("/echo") {
                    val message =
                        call.queryParameters["message"] ?: call.respond(HttpStatusCode.BadRequest)
                    call.respond(HttpStatusCode.OK, message)
                }
                get("/models") {
                }
            }
        }
    }
}
