package com.example.submito.dto

import com.example.submito.entity.Role

data class UserResponse(val id: Long, val name: String, val email: String, val role: Role)

data class AuthResponse(
        val accessToken: String,
        val tokenType: String = "Bearer",
        val user: UserResponse
)
