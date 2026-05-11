package com.example.submito.service

import com.example.submito.dto.UserResponse
import com.example.submito.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: UserRepository) {

    fun getAllUsers(): List<UserResponse> =
        userRepository.findAll().map { user ->
            UserResponse(
                id = user.id!!,
                name = user.name,
                email = user.email,
                role = user.role
            )
        }
}