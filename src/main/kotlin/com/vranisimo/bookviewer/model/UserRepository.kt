package com.vranisimo.bookviewer.model

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<User, String> {

    @Query("SELECT * FROM public.\"user\" WHERE email = :email")
    fun findUserByEmail(email: String): User?

    @Query("SELECT * FROM public.\"user\" WHERE username = :username")
    fun findUserByUsername(username: String): User?

}