package com.vranisimo.bookviewer

import com.vranisimo.bookviewer.model.*
import com.vranisimo.bookviewer.services.BookService
import com.vranisimo.bookviewer.services.GcsService
import com.vranisimo.bookviewer.services.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.*


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class BookViewerApplication

fun main(args: Array<String>) {
    runApplication<BookViewerApplication>(*args)
}

@RestController
@RequestMapping("/book")
class BookResource(val bookService: BookService, val userService: UserService) {

    val TOKEN_EXPIRATION_MS = 60 * 24 * 1000 // 24 hours

    val JWT_SECRET_KEY = "secret" // TODO change secret to something else

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping("/all", produces = ["application/json"])
    fun getAllBooks(@RequestParam token: String?): ResponseEntity<Any> {
        try {
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("authentication token is required")
            }

            val body = Jwts.parser().setSigningKey(JWT_SECRET_KEY).parseClaimsJws(token).body
//            body.issuer
            // TODO

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated")
        }
        return ResponseEntity.ok(booksToJsonResponse(bookService.findBooks()))
    }

    @GetMapping("", produces = ["application/json"])
    fun getBookByIsbn(@RequestParam(required = false) isbn: String): ResponseEntity<Any> {

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

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            logger.error("ISBN '$isbn' is not valid")
            return ResponseEntity.badRequest().body(ErrorMessage("Isbn '$isbn' is not valid"))
        }

        if (pageNumber == 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Page numbers starts from 1")
        }

        val isbnDigitsOnly = Utils.getIsbnDigitsOnly(isbn);

        // return a specific book
        val book: Book = bookService.findBook(isbnDigitsOnly)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorMessage("A book with ISBN '$isbn' is not found"))

        if (pageNumber > book.page_count) {
            logger.error("A book with ISBN '$isbn' does not contain a page $pageNumber")
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "A book with ISBN '$isbn' does not contain a page $pageNumber"
            )
        }
        if (!book.is_processed) {
            logger.error("A book with ISBN '$isbn' is not processed yet")
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "A book with ISBN '$isbn' is not processed yet"
            )
        }

        return ResponseEntity.ok(GcsService.getBookPageSignedUrl(isbnDigitsOnly, pageNumber))
    }

    @PostMapping("upload")
    fun upload(
        @RequestParam("pdfData") file: MultipartFile,
        @RequestParam isbn: String
    ): ResponseEntity<Any> {
        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            return ResponseEntity.badRequest().body(ErrorMessage("Isbn '$isbn' is not valid"))
        }

        val isbnDigitsOnly: String = Utils.getIsbnDigitsOnly(isbn)

        val bookFromDatabase: Book? = bookService.findBook(Utils.getIsbnDigitsOnly(isbn))
        if (bookFromDatabase != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorMessage("A book with ISBN '$isbn' is already uploaded"))
        }

        val book = Book(null, isbnDigitsOnly, 0, 0, false)

        // insert book row into the database
        bookService.storeBook(book)

        // store book PDF on object storage
        GcsService.storeBookPdf(isbnDigitsOnly, file.bytes)

        // TODO count PDF page size and store it into database
        // TODO send message to kafka queue

        return ResponseEntity.ok("File is successfully uploaded")
    }

    @PostMapping("register")
    fun register(@RequestBody body: UserDTO): ResponseEntity<Any> {
        logger.info("body:\n${body.username} ${body.email} ${body.password}")

        if (!validateUsername(body.username) || !validatePassword(body.password)) {
            return ResponseEntity.badRequest().body(ErrorMessage("An username or password is not valid"))
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
        if (!validateUsername(body.username) || !validatePassword(body.password)) {
            return ResponseEntity.badRequest().body(ErrorMessage("An username or password is not valid"))
        }

        val user: User = userService.findUserByUsername(body.username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorMessage("An user with username " + body.username + " does not exist"))

        // validates password from body
        if (!user.validatePassword(body.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorMessage("Incorrect password"))
        }

        val issuer = user.id.toString()

        val jwt = Jwts.builder()
            .setIssuer(issuer)
            .setExpiration(Date(System.currentTimeMillis() + TOKEN_EXPIRATION_MS))
            .signWith(SignatureAlgorithm.HS512, JWT_SECRET_KEY)
            .compact()

        return ResponseEntity.ok(jwt)
    }

    private fun validateUsername(username: String): Boolean {
        val MIN_LENGTH = 8
        val MAX_LENGTH = 64
        return (username.length >= MIN_LENGTH && username.length <= MAX_LENGTH)
    }

    private fun validatePassword(password: String): Boolean {
        val MIN_LENGTH = 8
        val MAX_LENGTH = 64
        return (password.length >= MIN_LENGTH && password.length <= MAX_LENGTH)
    }
}
