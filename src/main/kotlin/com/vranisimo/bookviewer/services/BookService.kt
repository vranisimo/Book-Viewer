package com.vranisimo.bookviewer.services

import com.vranisimo.bookviewer.model.Book
import com.vranisimo.bookviewer.repository.BookRepository
import org.springframework.stereotype.Service

@Service
class BookService(val db: BookRepository) {

    fun findBooks(): List<Book> = db.findBook()

    fun findBook(isbn: String): Book? {
        return db.findBook(isbn)
    }

    fun storeBook(book: Book) {
        db.save(book)
    }

    fun updateBook(book: Book) {
        db.updateBook(book.isbn, book.page_count, book.processed_page_count)
    }
}