package com.vranisimo.bookviewer.repository

import com.vranisimo.bookviewer.model.Book
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository


interface BookRepository : CrudRepository<Book, String> {

    @Query("SELECT * FROM Book")
    fun findBook(): List<Book>

    @Query("SELECT * FROM Book WHERE isbn = :isbn")
    fun findBook(isbn: String): Book?

    @Modifying
    @Query("UPDATE book SET page_count = :page_count, processed_page_count = :processed_page_count WHERE book.isbn = :isbn")
    fun updateBook(isbn: String, page_count : Int, processed_page_count : Int)
}