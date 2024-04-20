package byteboard.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

data class RateLimitConfig(var lastRequestTime: Long, var requestCount: Int)

val rateLimitMap = ConcurrentHashMap<String, RateLimitConfig>()

fun Application.configureRateLimiting() {

    intercept(Plugins) {
        val ip = call.request.origin.remoteHost
        val currentTime = System.currentTimeMillis()
        val rateLimitInfo = rateLimitMap.getOrPut(ip) { RateLimitConfig(currentTime, 0) }/*
        *Apply a certain rate limit to login and signup
         */
        if (call.request.uri == "/byteboard/login" || call.request.uri == "/byteboard/signup") {
            if (currentTime - rateLimitInfo.lastRequestTime > 1.minutes.inWholeMilliseconds) {
                rateLimitInfo.requestCount = 1
                rateLimitInfo.lastRequestTime = currentTime
            } else {
                rateLimitInfo.requestCount++
                if (rateLimitInfo.requestCount > 10) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf("Response" to "Too many requests, rate limit exceeded"),
                    )
                    finish()
                }
            }

            /*
            *Apply another rate limit to post creation, comment creation, message send etc
             */

        } else if (call.request.uri == "byteboard/posts/create" || call.request.uri == "/byteboard/comments/post" || call.request.uri == "/byteboard/messages/send") {

            if (currentTime - rateLimitInfo.lastRequestTime > 1.minutes.inWholeMilliseconds) {
                rateLimitInfo.requestCount = 1
                rateLimitInfo.lastRequestTime = currentTime
            } else {
                rateLimitInfo.requestCount++
                if (rateLimitInfo.requestCount > 6) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf("Response" to "Too many requests, rate limit exceeded"),
                    )
                    finish()
                }
            }


        } else {
            if (currentTime - rateLimitInfo.lastRequestTime > 1.minutes.inWholeMilliseconds) {
                rateLimitInfo.requestCount = 1
                rateLimitInfo.lastRequestTime = currentTime
            } else {
                rateLimitInfo.requestCount++
                if (rateLimitInfo.requestCount > 60) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf("Response" to "Too many requests, rate limit exceeded"),
                    )
                    finish()
                }
            }
        }
    }
}