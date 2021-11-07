package com.vranisimo.bookviewer.consumer

import com.google.gson.Gson
import com.vranisimo.bookviewer.model.Book
import com.vranisimo.bookviewer.model.ProcessPdfMessage
import com.vranisimo.bookviewer.producer.PdfProducer
import com.vranisimo.bookviewer.services.BookService
import com.vranisimo.bookviewer.services.GcsService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Service
class PdfConsumer(val bookService: BookService, val pdfProducer: PdfProducer) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @KafkaListener(topics = ["extract-pdf-page"], groupId = "group_id")
    fun consume(msg: String) {
        logger.info("CONSUMER: message received from topic : $msg")

        // deserialize JSON message
        val message = Gson().fromJson(msg, ProcessPdfMessage::class.java)

        val pageNumber: Int = message.page_number
        var totalPageCount = 0
        val isFirstRunForPdf: Boolean = (pageNumber == 1)

        // copy PDF file from GCS, we will remove it after processing is done
        val pdfFilePath = GcsService.downloadPdf(message.isbn).toString()

        // if it is a first message for this PDF, get total page count for PDF so that we know how many other messages to send to topic
        if (isFirstRunForPdf) {
            logger.info("Counting total number of pages in PDF:${message.isbn}")
            // use ImageMagick to count pages
            totalPageCount = countPages(pdfFilePath)
            if (totalPageCount < 1) {
                logger.error("Error while counting page from PDF file $pdfFilePath")
                return
            }
            logger.info("PDF ${message.isbn} contains $totalPageCount pages")
        }

        // extract PDF page and convert it to image
        val extractedImageFilename: String = message.isbn + "_" + pageNumber + ".jpeg"
        if (!extractImageToLocalFolder(pdfFilePath, pageNumber, extractedImageFilename)) {
            logger.error("An error occurred while converting PDF page to image. PDF ISBN: ${message.isbn}, page number: $pageNumber")
            return
        }

        // extractImageToLocalFolder method downloaded image to local folder
        val extractedImageFile = File(extractedImageFilename)

        // upload image file to GCS
        GcsService.uploadBookPage(message.isbn, pageNumber, extractedImageFile.readBytes())

        // update book record in the database
        val book: Book? = bookService.findBook(message.isbn)
        if (book != null) {
            if (isFirstRunForPdf) {
                book.page_count = totalPageCount
            }
            book.processed_page_count += 1 // increase processed_page_count for book

            logger.info("Book updated to database")
            bookService.updateBook(book) // store changes to database

            // if this was the first message for this PDF, send messages to process other pages
            if (isFirstRunForPdf && book.page_count > 1) {
                sendMessagesToKafka(book)
            }
        }

        // delete PDF and image files that were created just for processing purpose
        File(pdfFilePath).delete()
        extractedImageFile.delete()
    }

    fun sendMessagesToKafka(book: Book) {
        for (pageNumber in 2..book.page_count) {
            val message = ProcessPdfMessage(book.isbn, pageNumber)

            logger.info("CONSUMER: Sending message to topic")
            pdfProducer.sendMessage(message)
        }
    }

    fun countPages(pdfFilePath: String): Int {
        val countPagesCommand : String

        var isLocalEnvironment = System.getenv("IS_LOCAL_ENVIRONMENT")
        if (isLocalEnvironment != null && isLocalEnvironment.equals("true")) {
            countPagesCommand = "magick identify -density 12 $pdfFilePath"  // magick version 7
        } else {
            // running on docker
            countPagesCommand = "identify -density 12 $pdfFilePath"   // magick version 6
        }

        val process: Process = Runtime.getRuntime().exec(countPagesCommand)
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        val exitVal = process.waitFor()
        return if (exitVal == 0) {
            val totalPageNumber =
                reader.readText().lines().size - 1 // NOTE, we must exclude newline characted at the end
            logger.info("Total page count in PDF $pdfFilePath is $totalPageNumber")
            totalPageNumber
        } else {
            logger.info("An error occurred while count total page count from PDF $pdfFilePath, program exit value: $exitVal")
            -1
        }
    }

    fun extractImageToLocalFolder(pdfFilePath: String, pageNumber: Int, outputFilename: String): Boolean {
        val extractImageCommand : String
        val pageNumberFromZero = pageNumber - 1

        var isLocalEnvironment = System.getenv("IS_LOCAL_ENVIRONMENT")
        if (isLocalEnvironment != null && isLocalEnvironment.equals("true")) {
            extractImageCommand = "magick convert $pdfFilePath[$pageNumberFromZero] $outputFilename"  // magick version 7
        } else {
            // running on docker
            extractImageCommand = "convert $pdfFilePath[$pageNumberFromZero] $outputFilename"    // magick version 6
        }

        val process: Process = Runtime.getRuntime().exec(extractImageCommand)

        val exitVal = process.waitFor()
        return if (exitVal == 0) {
            logger.info("Page $pageNumber is successfully extracted from PDF $pdfFilePath")
            true
        } else {
            logger.info("An error occurred while extracting the $pageNumber. page from the PDF $pdfFilePath, program exit value: $exitVal")
            false
        }
    }
}