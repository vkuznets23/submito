package com.example.submito.service

import com.example.submito.dto.AuthResponse
import com.example.submito.dto.RegisterRequest
import com.example.submito.dto.UserResponse
import com.example.submito.entity.User
import com.example.submito.repository.UserRepository
import com.example.submito.security.jwt.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
        private val userRepository: UserRepository,
        private val passwordEncoder: PasswordEncoder,
        private val jwtService: JwtService
) {
    fun register(request: RegisterRequest): AuthResponse {
        // check if email is already in use or not
        if (userRepository.existsByEmail(request.email)) {
            throw RuntimeException("Email ${request.email} already exists")
        }
        val encodedPassword =
                passwordEncoder.encode(request.password)
                        ?: throw IllegalStateException("Password encoding failed")
        // create new user
        val user =
                User(
                        name = request.name,
                        email = request.email,
                        passwordHash = encodedPassword,
                        role = request.role
                )
        // save usee to db
        val savedUser = userRepository.save(user)
        // generate token
        val token = jwtService.generateToken(savedUser)
        // return auth response
        return AuthResponse(
                accessToken = token,
                "Bearer",
                user =
                        UserResponse(
                                id = savedUser.id!!,
                                name = savedUser.name,
                                email = savedUser.email,
                                role = savedUser.role
                        )
        )
    }
}
