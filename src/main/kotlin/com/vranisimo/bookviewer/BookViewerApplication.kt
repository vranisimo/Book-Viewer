package com.vranisimo.bookviewer

import com.vranisimo.bookviewer.model.Book
import com.vranisimo.bookviewer.model.bookToJsonResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@SpringBootApplication
class BookViewerApplication

fun main(args: Array<String>) {
    runApplication<BookViewerApplication>(*args)
}

@RestController
@RequestMapping("/book")
class BookResource(val service: BookService) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping("/all")
    fun getAllBooks(): List<Book> {
        return service.findBooks()
    }

    @GetMapping("", produces = ["application/json"])
    fun getBookByIsbn(@RequestParam(required = false) isbn: String): String {

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            logger.error("ISBN '$isbn' is not valid")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ISBN '$isbn' is not valid")
        }
        // return a specific book
        val book: Book = service.findBook(isbn)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A book with ISBN '$isbn' is not found")

        return bookToJsonResponse(book)
    }

    @PostMapping
    fun post(
        @RequestBody message: Book,
        @PathVariable isbn: String
    ) {

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Isbn '$isbn' is not valid")
        }

        service.storeBook(message)
    }
}
