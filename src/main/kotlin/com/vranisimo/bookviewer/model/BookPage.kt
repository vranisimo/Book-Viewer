package com.vranisimo.bookviewer.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("book_page")
data class BookPage(
    @Id val id: String?,
    val isbn: String,
    val page_number: Int
)