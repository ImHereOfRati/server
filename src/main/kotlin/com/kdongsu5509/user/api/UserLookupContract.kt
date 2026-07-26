package com.kdongsu5509.user.api

import java.util.*

interface UserLookupContract {
    fun findById(id: UUID): UserResult
    fun findByEmail(email: String): UserResult
    fun findByEmailOrNull(email: String): UserResult?
}
