package com.vranisimo.bookviewer.model

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository


interface BookRepository : CrudRepository<Book, String>{

    @Query("select * from Book")
    fun findBook(): List<Book>


    @Query("select * from Book where isbn = :isbn")
    fun findBook(isbn : String): Book?
}