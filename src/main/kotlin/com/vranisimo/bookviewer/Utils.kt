package com.vranisimo.bookviewer

import org.slf4j.LoggerFactory


class Utils {
    /**
     * A method validates if given isbn value is in ISBN13 format.
     * The validation contains of a regex and checksum check.
     */
    companion object {
        val logger = LoggerFactory.getLogger(this.javaClass)

        fun getIsbnDigitsOnly(isbn: String): String {
            return isbn
                .replace("ISBN-13: ", "")
                .replace(Regex("[- ]"), "")
                .replace("-", "")
        }

        fun isIsbnValidISBN13(isbn: String): Boolean {
            val regex =
                "^(?:ISBN(?:-13)?:?.)?(?=[0-9]{13}\$|(?=(?:[0-9]+[-.]){4})[-.0-9]{17}\$)97[89][-.]?[0-9]{1,5}[-.]?[0-9]+[-.]?[0-9]+[-.]?[0-9]\$".toRegex()

            // validate checksum
            val checksumIsValid = getIsbnDigitsOnly(isbn).map { it - '0' }
                .mapIndexed { index, value ->
                    when (index % 2) {
                        0 -> value
                        else -> 3 * value
                    }
                }
                .sum() % 10 == 0

            val isValid = regex.matches(isbn) && checksumIsValid
            if (!isValid) {
                logger.error("ISBN '$isbn' is not valid")
            }
            return isValid;
        }
    }
}