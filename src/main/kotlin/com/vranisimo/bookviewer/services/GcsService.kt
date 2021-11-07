package com.vranisimo.bookviewer.services

import com.google.auth.Credentials
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


object GcsService {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Value("\${spring.cloud.gcp.project-id}")
    private val GCS_PROJECT_ID: String = ""

//    private val storage: Storage = StorageOptions.getDefaultInstance().service
    // NOTE below two lines are workaround for the issue with fetching GCS credential filepath property
    val credentials: Credentials = GoogleCredentials.fromStream(FileInputStream("gcs_credentials.json"))
    val storage = StorageOptions.newBuilder().setCredentials(credentials).setProjectId(GCS_PROJECT_ID).build().service

    @Value("\${com.vranisimo.bookviewer.gcs.bucket.book}")
    private val BOOK_BUCKET_NAME: String = "book_pdf"

    @Value("\${com.vranisimo.bookviewer.gcs.bucket.bookpage}")
    private val BOOK_PAGE_BUCKET_NAME: String = "book_pages"

    private val PDF_EXTENSION = ".pdf"
    private val JPEG_EXTENSION = ".jpeg"

    fun storeBookPdf(isbn: String, pdfData: ByteArray) {
        uploadFile(BOOK_BUCKET_NAME, pdfData, isbn + PDF_EXTENSION)
    }

    fun uploadBookPage(isbn: String, pageNumber: Int, jpegData: ByteArray) {
        uploadFile(BOOK_PAGE_BUCKET_NAME, jpegData, isbn + "_" + pageNumber + JPEG_EXTENSION)
    }

    fun downloadPdf(isbn: String) : Path {
        val bucket = storage.get(BOOK_BUCKET_NAME) ?: error("Bucket $BOOK_BUCKET_NAME does not exist!")

        val pdfName = isbn + PDF_EXTENSION
        val blob = bucket.get(pdfName) ?: error("PDF $pdfName does not exist!")

        val localFilePath = Paths.get(pdfName)
        val writeTo = PrintStream(FileOutputStream(localFilePath.toFile()))
        writeTo.write(blob.getContent())
        writeTo.close()

        logger.info("$pdfName was successfully downloaded to $localFilePath.")
        return localFilePath
    }

    private fun uploadFile(bucketName: String, fileData: ByteArray, blobName: String) {
        val bucket = storage.get(bucketName)

        bucket.create(blobName, fileData)
        logger.info("$blobName was successfully uploaded to bucket $bucketName.")
    }

    fun getBookPageSignedUrl(isbn: String, pageNumber: Int): String {
        return getSignedUrl(BOOK_PAGE_BUCKET_NAME, getBookPageBlobName(isbn, pageNumber))
    }

    private fun getBookPageBlobName(isbn: String, pageNumber: Int): String {
        return isbn + "_" + pageNumber + JPEG_EXTENSION
    }

    private fun getSignedUrl(bucketName: String, blobName: String): String {
        val expiration: Long = 604800 // 7 days expiration time

        val bucket = storage.get(bucketName)
        val blob = bucket.get(blobName)

        //The query params
        val queryParams: MutableMap<String, String> = HashMap()
        queryParams["response-content-disposition"] = "inline"
        queryParams["response-content-type"] = "image/jpeg"

        val signUrl = storage.signUrl(
            blob, expiration, TimeUnit.MILLISECONDS,
            Storage.SignUrlOption.withQueryParams(queryParams), //This is the magic line
            Storage.SignUrlOption.withV4Signature()
        )

        logger.info("Expired signed URL:\n$signUrl")
        return signUrl.toString()
    }
}