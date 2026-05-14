package com.example.submito.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp

// USER:
// id: Long? = null,
// name: String,
// role: Role = Role.STUDENT,
// email: String,
// passwordHash: String,
// createdAt: LocalDateTime = LocalDateTime.now(),
// updatedAt: LocalDateTime = LocalDateTime.now(),
@Entity // JPA annotation to make this class a JPA entity
@Table(name = "users") // table name in the db
class User(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
        @Column(nullable = false) val name: String,
        @Enumerated(EnumType.STRING) @Column(nullable = false) val role: Role = Role.STUDENT,
        @Column(nullable = false, unique = true) val email: String,
        @Column(nullable = false) val passwordHash: String,
        @CreationTimestamp val createdAt: LocalDateTime = LocalDateTime.now(),
        @UpdateTimestamp val updatedAt: LocalDateTime = LocalDateTime.now(),
)
