package com.vranisimo.bookviewer.services

import com.vranisimo.bookviewer.model.User
import com.vranisimo.bookviewer.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(val db: UserRepository) {

    fun getUserById(id : Int) = db.findById(id.toString())

    fun findUserByUsername(username: String): User? = db.findUserByUsername(username)

    fun findUserByEmail(email: String): User? = db.findUserByEmail(email)

    fun storeUser(user: User) {
        db.save(user)
    }
}