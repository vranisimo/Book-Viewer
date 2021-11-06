package com.vranisimo.bookviewer.model

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Table("user")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0

    @Column(unique = true)
    var username = ""

    @Column(unique = true)
    var email = ""

    @Column
    var password = ""
        @JsonIgnore
        get() = field
        set(value) {
            val passwordEncoder = BCryptPasswordEncoder()
            field = passwordEncoder.encode(value)
        }

    fun validatePassword(passwordToValidate: String): Boolean {
        return BCryptPasswordEncoder().matches(passwordToValidate, this.password)
    }
}