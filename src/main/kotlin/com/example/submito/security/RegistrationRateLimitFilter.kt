package com.example.submito.security

import com.example.submito.dto.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.json.JsonMapper


@Component
class RegistrationRateLimitFilter(
    private val jsonMapper: JsonMapper
) : OncePerRequestFilter() {

    // e.g. 5 registration attempts per IP per 15 minutes
    private val maxRequests = 5
    private val windowSeconds = 15 * 60L

    // IP -> list of request timestamps (milliseconds)
    private val requestsByIp = ConcurrentHashMap<String, MutableList<Long>>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Only limit registration
        if (request.method != "POST" || !request.requestURI.endsWith("/auth/register")) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = resolveClientIp(request)
        if (isRateLimited(clientIp)) {
            writeTooManyRequests(response, request.servletPath)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isRateLimited(clientIp: String): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = now - windowSeconds * 1000

        val timestamps = requestsByIp.computeIfAbsent(clientIp) { mutableListOf() }

        synchronized(timestamps) {
            timestamps.removeIf { it < windowStart }

            if (timestamps.size >= maxRequests) {
                return true
            }

            timestamps.add(now)
            return false
        }
    }

    private fun resolveClientIp(request: HttpServletRequest): String {
        // For local dev this is enough. Behind a trusted proxy you'd use X-Forwarded-For carefully.
        return request.remoteAddr ?: "unknown"
    }

    private fun writeTooManyRequests(response: HttpServletResponse, path: String) {
        val status = HttpStatus.TOO_MANY_REQUESTS

        val body = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = "Too many registration attempts. Please try again later.",
            path = path,
            timestamp = Instant.now()
        )

        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(jsonMapper.writeValueAsString(body))
    }
}