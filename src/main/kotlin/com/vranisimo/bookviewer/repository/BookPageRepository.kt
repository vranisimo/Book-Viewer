package com.vranisimo.bookviewer.repository

import com.vranisimo.bookviewer.model.BookPage
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface BookPageRepository : CrudRepository<BookPage, String> {

    @Query("SELECT * FROM BookPage WHERE isbn = :isbn AND page_number = :pageNumber")
    fun findBookPage(isbn: String, pageNumber : Int): BookPage?
}