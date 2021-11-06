package com.vranisimo.bookviewer.model

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ErrorMessage(public val errorMessage: String) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    init {
        // log error message when it is returned to the response
        logger.error(errorMessage)
    }
}