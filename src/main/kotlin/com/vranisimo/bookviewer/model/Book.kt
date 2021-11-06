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
    val gsonPretty = GsonBuilder().setPrettyPrinting().create()
    return gsonPretty.toJson(book)
}

fun booksToJsonResponse(books: List<Book>): String {
    val gsonPretty = GsonBuilder().setPrettyPrinting().create()
    return gsonPretty.toJson(books)
}