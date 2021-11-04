package com.vranisimo.bookviewer.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("book")
data class Book(
    @Transient
    @Id var id: String?,
    var isbn: String,

    @Transient
    var pdf: String,

    var page_count: Int,

    var processed_page_count: Int,
    var is_processed: Boolean
)

fun bookToJsonResponse(book : Book) : String{
//    val gson = Gson()
//    val jsonTut: String = gson.toJson(book,  )
//    println(jsonTut)

    val gsonPretty = GsonBuilder().setPrettyPrinting().create()
    val jsonTutPretty: String = gsonPretty.toJson(book)
//    println(jsonTutPretty)
    return jsonTutPretty
}