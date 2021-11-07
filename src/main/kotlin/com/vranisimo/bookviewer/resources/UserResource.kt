package com.vranisimo.bookviewer.resources

import com.vranisimo.bookviewer.Utils
import com.vranisimo.bookviewer.jwt.TokenUtil
import com.vranisimo.bookviewer.model.*
import com.vranisimo.bookviewer.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user")
class UserResource(val userService: UserService) {

    @Value("\${com.vranisimo.bookviewer.username.minlength}")
    val USERNAME_MIN_LENGTH: Int = 8

    @Value("\${com.vranisimo.bookviewer.username.maxlength}")
    val USERNAME_MAX_LENGTH: Int = 64

    @Value("\${com.vranisimo.bookviewer.password.minlength}")
    val PASSWORD_MIN_LENGTH: Int = 8

    @Value("\${com.vranisimo.bookviewer.password.maxlength}")
    val PASSWORD_MAX_LENGTH: Int = 64

    @Autowired
    private val tokenUtils: TokenUtil? = null

    @PostMapping("register")
    fun register(@RequestBody body: UserDTO): ResponseEntity<Any>? {
        // NOTE no need to validate token here

        if (validateUsernameAndPassword(body.username, body.password) != null) {
            return validateUsernameAndPassword(body.username, body.password)
        }

        // validate email address
        if (!Utils.isEmailValid(body.email)) {
            return ResponseEntity.badRequest().body(ErrorMessage("Email is not valid"))
        }

        // check if user with the same username already exist
        if (userService.findUserByUsername(body.username) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorMessage("An user with username " + body.username + " already exist"))
        }

        // check if user with the same email already exist
        if (userService.findUserByEmail(body.email) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorMessage("An user with email " + body.email + "already exist "))
        }

        val user = User()
        user.username = body.username
        user.email = body.email
        user.password = body.password

        this.userService.storeUser(user)

        return ResponseEntity.ok("Registration finished successfully")
    }

    @PostMapping("login")
    fun login(
        @RequestBody body: LoginDTO
    ): ResponseEntity<Any>? {
        // NOTE no need to validate token here

        if (validateUsernameAndPassword(body.username, body.password) != null) {
            return validateUsernameAndPassword(body.username, body.password)
        }

        // check if user exist
        val user: User = userService.findUserByUsername(body.username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorMessage("An user with username " + body.username + " does not exist"))

        // check if provided and database password matches
        if (!user.validatePassword(body.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorMessage("Incorrect password"))
        }

        val accessToken = tokenUtils?.generateToken(user)
            ?: return ResponseEntity.badRequest().body(ErrorMessage("An error occurred during the login"))

        // return access token
        val token = TokenDTO(accessToken)
        return ResponseEntity.ok(token)
    }

    /**
     * Returns ResponseEntity if failed
     */
    private fun validateUsernameAndPassword(username: String, password: String): ResponseEntity<Any>? {
        val usernameValidation = validateUsername(username)
        val passwordValidation = validatePassword(password)
        if (usernameValidation.isNotBlank()) return ResponseEntity.badRequest().body(ErrorMessage(usernameValidation))
        if (passwordValidation.isNotBlank()) return ResponseEntity.badRequest().body(ErrorMessage(passwordValidation))
        return null
    }

    private fun validateUsername(username: String): String {
        if (username.isBlank()) return "Username is not defined"
        return if (username.length !in USERNAME_MIN_LENGTH..USERNAME_MAX_LENGTH) "Username length must be between $USERNAME_MIN_LENGTH and $USERNAME_MAX_LENGTH characters." else ""
    }

    private fun validatePassword(password: String): String {
        if (password.isBlank()) return "Password is not defined"
        return if (password.length !in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH) "Password length must be between $PASSWORD_MIN_LENGTH and $PASSWORD_MAX_LENGTH characters." else ""
    }
}
