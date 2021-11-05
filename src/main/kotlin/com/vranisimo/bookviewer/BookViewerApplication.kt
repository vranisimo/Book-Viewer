package com.vranisimo.bookviewer

import com.vranisimo.bookviewer.model.Book
import com.vranisimo.bookviewer.model.bookToJsonResponse
import com.vranisimo.bookviewer.model.booksToJsonResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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

    @GetMapping("/all", produces = ["application/json"])
    fun getAllBooks(): String {
        return booksToJsonResponse(service.findBooks())
    }

    @GetMapping("", produces = ["application/json"])
    fun getBookByIsbn(@RequestParam(required = false) isbn: String): String {

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ISBN '$isbn' is not valid")
        }
        // return a specific book
        val book: Book = service.findBook(Utils.getIsbnDigitsOnly(isbn))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A book with ISBN '$isbn' is not found")

        return bookToJsonResponse(book)
    }

    @GetMapping("getUrl")
    fun getExpirableUrl(
        @RequestParam(required = true) isbn: String,
        @RequestParam(required = true) pageNumber: Int
    ): String {

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            logger.error("ISBN '$isbn' is not valid")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ISBN '$isbn' is not valid")
        }

        val isbnDigitsOnly = Utils.getIsbnDigitsOnly(isbn);

        // return a specific book
        val book: Book = service.findBook(isbnDigitsOnly)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "A book with ISBN '$isbn' is not found")

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

        return GcsService.getBookPageSignedUrl(isbnDigitsOnly, pageNumber)
    }

    @PostMapping("upload")
    fun upload(
        @RequestParam("pdfData") file: MultipartFile,
        @RequestParam(required = true) isbn: String
    ) {
        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Isbn '$isbn' is not valid")
        }

        val isbnDigitsOnly: String = Utils.getIsbnDigitsOnly(isbn)

        val bookFromDatabase: Book? = service.findBook(Utils.getIsbnDigitsOnly(isbn))
        if (bookFromDatabase != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A book with ISBN '$isbn' is already uploaded")
        }

        val book = Book(null, isbnDigitsOnly, 0, 0, false)

        // insert book row into the database
        service.storeBook(book)

        // store book PDF on object storage
        GcsService.storeBookPdf(isbnDigitsOnly, file.bytes)

        // TODO count PDF page size and store it into database
        // TODO send message to kafka queue
    }
}
