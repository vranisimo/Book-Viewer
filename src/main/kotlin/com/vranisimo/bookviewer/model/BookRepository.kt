package com.vranisimo.bookviewer.model

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface BookRepository : CrudRepository<Book, String> {

    @Query("SELECT * FROM Book")
    fun findBook(): List<Book>

    @Query("SELECT * FROM Book WHERE isbn = :isbn")
    fun findBook(isbn: String): Book?
}