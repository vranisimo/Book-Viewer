package com.vranisimo.bookviewer

import com.vranisimo.bookviewer.model.Book
import com.vranisimo.bookviewer.model.BookRepository
import org.springframework.stereotype.Service

@Service
class BookService(val db: BookRepository) {

    fun findBooks(): List<Book> = db.findBook()

    fun findBook(isbn : String) : Book?{
        return db.findBook(isbn)
    }

    fun storeBook(book: Book) {
        db.save(book)
    }
}