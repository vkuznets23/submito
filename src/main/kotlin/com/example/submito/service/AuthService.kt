package com.example.submito.service

import com.example.submito.dto.AuthResponse
import com.example.submito.dto.RegisterRequest
import com.example.submito.dto.UserResponse
import com.example.submito.entity.Role
import com.example.submito.entity.RegisterRole
import com.example.submito.entity.User
import com.example.submito.exception.EmailAlreadyExistsException
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
        val normalizedEmail = request.email.lowercase().trim()
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw EmailAlreadyExistsException(normalizedEmail)
        }
        val encodedPassword =
                passwordEncoder.encode(request.password)
                        ?: throw IllegalStateException("Password encoding failed")

        val userRole = when (request.role) {
            RegisterRole.STUDENT -> Role.STUDENT
            RegisterRole.TEACHER -> Role.TEACHER
        }
        // create new user
        val user =
                User(
                        name = request.name,
                        email = normalizedEmail,
                        passwordHash = encodedPassword,
                        role = userRole
                )
        // save user to db
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
