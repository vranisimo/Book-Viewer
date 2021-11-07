package com.vranisimo.bookviewer.model

import com.google.gson.GsonBuilder
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Table

@Table("book")
data class Book(
    @Transient
    @Id var id: String?,
    var isbn: String,

    var page_count: Int,
    var processed_page_count: Int,

    @ReadOnlyProperty
    var is_processed: Boolean
)

fun bookToJsonResponse(book: Book): String {
    return GsonBuilder().setPrettyPrinting().create().toJson(book)
}

fun booksToJsonResponse(books: List<Book>): String {
    return GsonBuilder().setPrettyPrinting().create().toJson(books)
}