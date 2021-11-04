package com.vranisimo.bookviewer

class Utils {

    /**
     * A method validates if given isbn value is in ISBN13 format.
     * The validation contains of a regex and checksum check.
     */
    companion object {
        fun isIsbnValidISBN13(isbn: String): Boolean {
            val regex =
                "^(?:ISBN(?:-13)?:?.)?(?=[0-9]{13}\$|(?=(?:[0-9]+[-.]){4})[-.0-9]{17}\$)97[89][-.]?[0-9]{1,5}[-.]?[0-9]+[-.]?[0-9]+[-.]?[0-9]\$".toRegex()

            val isbnDigitsOnly = isbn
                .replace("ISBN-13: ", "")
                .replace(Regex("[- ]"), "")
                .replace("-", "")

            val checksumIsValid = isbnDigitsOnly.map { it - '0' }
                .mapIndexed { index, value ->
                    when (index % 2) {
                        0 -> value
                        else -> 3 * value
                    }
                }
                .sum() % 10 == 0

            return regex.matches(isbn) && checksumIsValid
        }
    }
}