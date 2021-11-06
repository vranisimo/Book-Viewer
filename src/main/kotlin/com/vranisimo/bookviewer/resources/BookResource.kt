package com.vranisimo.bookviewer.resources

import com.vranisimo.bookviewer.Utils
import com.vranisimo.bookviewer.model.Book
import com.vranisimo.bookviewer.model.ErrorMessage
import com.vranisimo.bookviewer.model.bookToJsonResponse
import com.vranisimo.bookviewer.model.booksToJsonResponse
import com.vranisimo.bookviewer.services.BookService
import com.vranisimo.bookviewer.services.GcsService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/book")
class BookResource(val bookService: BookService) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping("", produces = ["application/json"])
    fun getBookByIsbn(@RequestParam(required = false) isbn: String?): ResponseEntity<Any> {
        // TODO move outside
        if (SecurityContextHolder.getContext().authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        // of no ISBN is provided, return all books
        if (isbn == null){
            return ResponseEntity.ok(booksToJsonResponse(bookService.findBooks()))
        }

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            return ResponseEntity.badRequest().body(ErrorMessage("ISBN '$isbn' is not valid"))
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
        if (SecurityContextHolder.getContext().authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            return ResponseEntity.badRequest().body(ErrorMessage("ISBN '$isbn' is not valid"))
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
        @RequestParam("pdfFile", required = false) file: MultipartFile?,
        @RequestParam isbn: String
    ): ResponseEntity<Any> {
        // TODO move outside
        if (SecurityContextHolder.getContext().authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (file == null){
            return ResponseEntity.badRequest().body(ErrorMessage("PDF file is missing"))
        }

        // validate isbn
        if (!Utils.isIsbnValidISBN13(isbn)) {
            return ResponseEntity.badRequest().body(ErrorMessage("ISBN '$isbn' is not valid"))
        }

        val isbnDigitsOnly: String = Utils.getIsbnDigitsOnly(isbn)

        // check if a book with ISBN already exist
        val bookFromDatabase: Book? = bookService.findBook(Utils.getIsbnDigitsOnly(isbn))
        if (bookFromDatabase != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorMessage("A book with ISBN '$isbn' is already uploaded"))
        }

        // allow PDF files only
        if (!file.name.endsWith(".pdf")){
            return ResponseEntity.badRequest().body(ErrorMessage("Only PDF files are allowed for upload"))
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
}