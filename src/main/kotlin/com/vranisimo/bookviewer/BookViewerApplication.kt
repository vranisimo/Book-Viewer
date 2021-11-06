package com.vranisimo.bookviewer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class BookViewerApplication

fun main(args: Array<String>) {
    runApplication<BookViewerApplication>(*args)
}