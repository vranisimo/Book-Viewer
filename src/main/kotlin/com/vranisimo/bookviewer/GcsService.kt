package com.vranisimo.bookviewer

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

val storage: Storage = StorageOptions.getDefaultInstance().service

var BOOK_BUCKET = "book-pdf"
var BOOK_PAGE_BUCKET = "book_pages"

fun uploadFile(bucketName: String, localFilePath: String, blobName: String) {
    val file = Paths.get(localFilePath)
    val bucket = storage.get(bucketName)

    bucket.create(blobName, Files.readAllBytes(file))
    println("$blobName was successfully uploaded to bucket $bucketName.")
}

fun getSignedUrl(bucketName: String, blobName: String): String {

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

    println(signUrl.toString())
    return signUrl.toString()
}

/**
 * Examples
 * upload test.txt book-pdf
 * getSignedUrl test.txt book-pdf
 */
fun main(vararg args: String) {
    uploadFile(BOOK_BUCKET, "test.jpeg", "test.jpeg")
    getSignedUrl(BOOK_BUCKET, "test.jpeg")
}