package com.vranisimo.bookviewer.model

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Message(public val message: String) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    init {
        // log info message when it is returned to the response
        logger.info(message)
    }
}