package com.vranisimo.bookviewer.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("book_page")
data class BookPage(
    @Transient
    @Id var id: String?,
    var isbn: String,
    var page_number: Int
)