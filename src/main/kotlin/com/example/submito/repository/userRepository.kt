package com.example.submito.repository

import com.example.submito.entity.Role
import com.example.submito.entity.User
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    // dont add save(), deleteBy(), findById() etc. methods here,
    // they are already defined in JpaRepository
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun findByRole(role: Role): List<User>
}
