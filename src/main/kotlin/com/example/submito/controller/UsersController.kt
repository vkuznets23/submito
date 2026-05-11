package com.example.submito.controller

import com.example.submito.dto.UserResponse
import com.example.submito.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UsersController (private val userService: UserService) {
    @GetMapping
        fun getAll(): List<UserResponse> = userService.getAllUsers()
}