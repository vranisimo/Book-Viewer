package com.vranisimo.bookviewer

import com.vranisimo.bookviewer.jwt.TokenUtil
import com.vranisimo.bookviewer.model.*
import com.vranisimo.bookviewer.services.BookService
import com.vranisimo.bookviewer.services.GcsService
import com.vranisimo.bookviewer.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class BookViewerApplication

fun main(args: Array<String>) {
    runApplication<BookViewerApplication>(*args)
}

@RestController
@RequestMapping("/book")
class BookResource(val bookService: BookService, val userService: UserService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    companion object {
        @Value("\${com.vranisimo.bookviewer.username.minlength}")
        const val USERNAME_MIN_LENGTH: Int = 8

        @Value("\${com.vranisimo.bookviewer.username.maxlength}")
        const val USERNAME_MAX_LENGTH: Int = 64

        @Value("\${com.vranisimo.bookviewer.password.minlength}")
        const val PASSWORD_MIN_LENGTH: Int = 8

        @Value("\${com.vranisimo.bookviewer.password.maxlength}")
        const val PASSWORD_MAX_LENGTH: Int = 64
    }

    @Autowired
    private val tokenUtils: TokenUtil? = null

    @GetMapping("/all", produces = ["application/json"])
    fun getAllBooks(@RequestHeader(value = "Authorization") token: String?): ResponseEntity<Any> {
        // TODO move outside
        if (SecurityContextHolder.getContext().authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return ResponseEntity.ok(booksToJsonResponse(bookService.findBooks()))
    }

    @GetMapping("", produces = ["application/json"])
    fun getBookByIsbn(@RequestParam(required = false) isbn: String): ResponseEntity<Any> {
        // TODO move outside
        if (SecurityContextHolder.getContext().authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            return ResponseEntity.badRequest().body(ErrorMessage("Isbn '$isbn' is not valid"))
        }
        // return a specific book
        val book: Book = bookService.findBook(Utils.getIsbnDigitsOnly(isbn))
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorMessage("A book with ISBN '$isbn' is not found"))

        return ResponseEntity.ok(bookToJsonResponse(book))
    }

    @GetMapping("getUrl")
    fun getExpirableUrl(
        @RequestParam isbn: String,
        @RequestParam pageNumber: Int
    ): ResponseEntity<Any> {
        // TODO move outside
        if (SecurityContextHolder.getContext().authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            return ResponseEntity.badRequest().body(ErrorMessage("Isbn '$isbn' is not valid"))
        }

        if (pageNumber == 0) {
            return ResponseEntity.badRequest()
                .body(ErrorMessage("Incorrect page number $pageNumber. Page number starts from 1"))
        }

        val isbnDigitsOnly = Utils.getIsbnDigitsOnly(isbn);

        // return a specific book
        val book: Book = bookService.findBook(isbnDigitsOnly)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorMessage("A book with ISBN '$isbn' is not found"))

        if (pageNumber > book.page_count) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorMessage("A book with ISBN '$isbn' does not contain a page $pageNumber"))
        }
        if (!book.is_processed) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorMessage("A book with ISBN '$isbn' is not processed yet"))
        }

        return ResponseEntity.ok(GcsService.getBookPageSignedUrl(isbnDigitsOnly, pageNumber))
    }

    @PostMapping("upload")
    fun upload(
        @RequestParam("pdfData") file: MultipartFile,
        @RequestParam isbn: String
    ): ResponseEntity<Any> {
        // TODO move outside
        if (SecurityContextHolder.getContext().authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            return ResponseEntity.badRequest().body(ErrorMessage("Isbn '$isbn' is not valid"))
        }

        val isbnDigitsOnly: String = Utils.getIsbnDigitsOnly(isbn)

        // check if a book with ISBN already exist
        val bookFromDatabase: Book? = bookService.findBook(Utils.getIsbnDigitsOnly(isbn))
        if (bookFromDatabase != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorMessage("A book with ISBN '$isbn' is already uploaded"))
        }

        // insert book row into the database
        val book = Book(null, isbnDigitsOnly, 0, 0, false)
        bookService.storeBook(book)

        // store book PDF on object storage
        GcsService.storeBookPdf(isbnDigitsOnly, file.bytes)

        // TODO count PDF page size and store it into database
        // TODO send message to kafka queue

        return ResponseEntity.ok("File is successfully uploaded")
    }

    @PostMapping("register")
    fun register(@RequestBody body: UserDTO): ResponseEntity<Any>? {
        // NOTE no need to validate token here

        if (validateUsernameAndPassword(body.username, body.password) != null){
            return validateUsernameAndPassword(body.username, body.password)
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

        if (validateUsernameAndPassword(body.username, body.password) != null){
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
    private fun validateUsernameAndPassword(username: String, password: String) : ResponseEntity<Any>?{
        val usernameValidation = validateUsername(username)
        val passwordValidation = validatePassword(password)
        if (usernameValidation.isNotBlank()) return ResponseEntity.badRequest().body(ErrorMessage(usernameValidation))
        if (passwordValidation.isNotBlank()) return ResponseEntity.badRequest().body(ErrorMessage(passwordValidation))
        return null
    }

    private fun validateUsername(username: String): String {
        if (username == null) return "Username is not defined"
        return if (username.length !in USERNAME_MIN_LENGTH..USERNAME_MAX_LENGTH) "Username length must be between $USERNAME_MIN_LENGTH and $USERNAME_MAX_LENGTH characters." else ""
    }

    private fun validatePassword(password: String): String {
        if (password == null) return "Password is not defined"
        return if (password.length !in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH) "Password length must be between $PASSWORD_MIN_LENGTH and $PASSWORD_MAX_LENGTH characters." else ""
    }
}
