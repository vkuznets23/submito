package com.example.submito.security.jwt

import com.example.submito.entity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import javax.crypto.SecretKey
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class JwtService {
    // @Value - injects the value of the property from application.yml into the field
    // private lateinit var - means that the field is initialized by Spring
    @Value("\${spring.jwt.secret}") private lateinit var secret: String
    @Value("\${spring.jwt.expiration}") private var expiration: Long = 0

    // make hashed key from secret string
    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(user: User): String {
        return Jwts.builder()
                .subject(user.email) // put email to "sub"
                .claim("role", user.role.name) // put role to "role"
                .claim("id", user.id) // put id to "id"
                .issuedAt(Date()) // put issued at time to "iat"
                .expiration(
                        Date(System.currentTimeMillis() + expiration)
                ) // put expiration time to "exp"
                .signWith(getSigningKey()) // sign with secret key
                .compact()
    }

    private fun extractClaims(token: String): Claims {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).payload
    }

    fun extractEmail(token: String): String {
        return extractClaims(token).subject
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = extractClaims(token)
            claims.expiration.after(Date())
        } catch (e: Exception) {
            false
        }
    }
}
