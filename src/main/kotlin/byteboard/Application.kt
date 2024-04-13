package byteboard

import byteboard.configuration.*
import byteboard.routing.comments.configureCommentsRouting
import byteboard.routing.account.configureProfileChanges
import byteboard.routing.account.configureUserProfileRouting
import byteboard.routing.posts.configurePostsRouting
import byteboard.routing.privatemessages.configureMessageRoutes
import byteboard.security.configureRateLimiting
import configureAdminPanelRouting
import configureLogin
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 6969, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureDatabase()
    configureRouting()
    configurePostsRouting()
    configureRateLimiting()
    configureLogin()
    configureProfileChanges()
    configureCommentsRouting()
    configureAdminPanelRouting()
    configureMessageRoutes()
    configureUserProfileRouting()
}
