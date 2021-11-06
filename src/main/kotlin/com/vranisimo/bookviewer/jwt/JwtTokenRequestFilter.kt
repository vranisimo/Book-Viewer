package com.vranisimo.bookviewer.jwt

import com.vranisimo.bookviewer.model.User
import com.vranisimo.bookviewer.services.UserService
import io.jsonwebtoken.ExpiredJwtException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class JwtTokenRequestFilter : OncePerRequestFilter() {

    @Autowired
    private val userService: UserService? = null

    @Autowired
    private val tokenUtil: TokenUtil? = null

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {

        val logger = LoggerFactory.getLogger(this.javaClass)

        // get token from request header
        val tokenHeaderFromRequest = request.getHeader("Authorization")
        var username: String? = null
        var token: String? = null

        if (tokenHeaderFromRequest == null) {
            logger.warn("Token header is empty")
        } else {
            // remove "Bearer " from the start
            token = tokenHeaderFromRequest.replace("Bearer ", "")

            try {
                username = tokenUtil?.getUsernameFromToken(token)
            } catch (e: IllegalArgumentException) {
                logger.error("Unable to get JWT Token", e)
            } catch (e: ExpiredJwtException) {
                logger.error("Token expired")
            } catch (e: Exception) {
                logger.error("An error occurred while parsing the token body\n", e)
            }
        }

        // validate token
        if (username != null) {
            val user: User? = userService?.findUserByUsername(username)

            if (user != null && tokenUtil?.validateToken(token, user) == true) {
                val usernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken(
                    user, null, null
                )
                usernamePasswordAuthenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                // mark current user as authenticated in Spring Security
                SecurityContextHolder.getContext().authentication = usernamePasswordAuthenticationToken
                logger.info("Token is valid")
            }
        }
        chain.doFilter(request, response)
    }
}