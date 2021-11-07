package com.vranisimo.bookviewer.jwt

import com.vranisimo.bookviewer.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.*


@Component
class TokenUtil : Serializable {
    @Value("\${jwt.secret}")
    private val jwtSecret: String? = null

    @Value("\${com.vranisimo.bookviewer.jwt.tokenexpirationminutes}")
    private val jwtTokenValidityMinutes: Int = 60 // use 1 hour as default if not set in properties

    fun getUsernameFromToken(token: String?): String {
        return getTokenClaimsBody(token).subject
    }

    fun getExpirationDateFromToken(token: String?): Date {
        return getTokenClaimsBody(token).expiration
    }

    private fun getTokenClaimsBody(token: String?): Claims {
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).body
    }

    private fun isTokenExpired(token: String?): Boolean {
        val expiration = getExpirationDateFromToken(token)
        return expiration.before(Date())
    }

    fun generateToken(user: User): String {
        val tokenValidatyInMs = jwtTokenValidityMinutes * 60 * 1000
        return Jwts.builder()
            .setIssuer(user.id.toString())  // issues is the User id from database
            .setSubject(user.username)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + tokenValidatyInMs))
            .signWith(SignatureAlgorithm.HS512, jwtSecret).compact()
    }

    fun validateToken(token: String?, user: User): Boolean {
        val username = getUsernameFromToken(token)
        return username == user.username && !isTokenExpired(token)
    }
}