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
class AuthRateLimitFilter(
    private val jsonMapper: JsonMapper
) : OncePerRequestFilter() {

    // e.g. 5 registration attempts per IP per 15 minutes
    private val registerMax = 5
    private val loginMax = 10
    private val windowSeconds = 15 * 60L

    private val registerByIp = ConcurrentHashMap<String, MutableList<Long>>()
    private val loginByIp = ConcurrentHashMap<String, MutableList<Long>>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val isRegister = request.method == "POST" && request.requestURI.endsWith("/auth/register")
        val isLogin = request.method == "POST" && request.requestURI.endsWith("/auth/login")

        if (!isRegister && !isLogin) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = resolveClientIp(request)
        if (isRegister) {
            if (isRateLimited(clientIp, registerByIp, registerMax)) {
                writeTooManyRequests(response, request.servletPath)
                return
            }
        } else if (isLogin) {
            if (isRateLimited(clientIp, loginByIp, loginMax)) {
                writeTooManyRequests(response, request.servletPath)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun isRateLimited(
    clientIp: String,
    store: ConcurrentHashMap<String, MutableList<Long>>,
    maxRequests: Int
    ): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = now - windowSeconds * 1000

        val timestamps = store.computeIfAbsent(clientIp) { mutableListOf() }

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
            message = "Too many attempts. Please try again later.",
            path = path,
            timestamp = Instant.now()
        )

        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(jsonMapper.writeValueAsString(body))
    }
}