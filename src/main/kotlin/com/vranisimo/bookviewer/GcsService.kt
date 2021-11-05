package com.vranisimo.bookviewer

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import java.util.concurrent.TimeUnit

object GcsService {
    private val storage: Storage = StorageOptions.getDefaultInstance().service

    private var BOOK_BUCKET = "book-pdf"
    private var BOOK_PAGE_BUCKET = "book_pages"

    private var PDF_EXTENSION = ".pdf"
    private var JPEG_EXTENSION = ".jpeg"

    fun storeBookPdf(isbn: String, pdfData: ByteArray) {
        uploadFile(BOOK_BUCKET, pdfData, isbn + PDF_EXTENSION)
    }

    // TODO use in consumers
    fun storeBookPage(isbn: String, pageNumber: Int, jpegData: ByteArray) {
        uploadFile(BOOK_PAGE_BUCKET, jpegData, isbn + "_" + pageNumber + JPEG_EXTENSION)
    }

    private fun uploadFile(bucketName: String, fileData: ByteArray, blobName: String) {
        val bucket = storage.get(bucketName)

        bucket.create(blobName, fileData)
        println("$blobName was successfully uploaded to bucket $bucketName.")
    }

    fun getBookPageSignedUrl(isbn: String, pageNumber: Int): String {
        return getSignedUrl(BOOK_PAGE_BUCKET, getBookPageBlobName(isbn, pageNumber))
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

        println("Expired signed URL:\n$signUrl")
        return signUrl.toString()
    }

    /**
     * Examples
     * upload test.txt book-pdf
     * getSignedUrl test.txt book-pdf
     */
//    fun main(vararg args: String) {
//        uploadFile(BOOK_BUCKET, "test.jpeg", "test.jpeg")
//        getSignedUrl(BOOK_BUCKET, "test.jpeg")
//    }
}