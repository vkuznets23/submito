package com.example.submito.dto

import com.example.submito.entity.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
        @field:NotBlank(message = "Name is required")
        @field:Size(min = 2, message = "Name must be at least 2 characters")
        val name: String,

        @field:NotBlank(message = "Email is required")
        @field:Email(message = "Email is invalid")
        val email: String,

        @field:NotBlank(message = "Password is required")
        @field:Size(min = 8, message = "Password must be at least 8 characters")
        @field:Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message =
                        "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        val password: String,
        
        @field:NotNull(message = "Role is required") val role: Role = Role.STUDENT
) {}
