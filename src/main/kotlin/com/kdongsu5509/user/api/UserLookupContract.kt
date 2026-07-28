package com.kdongsu5509.user.api

import java.util.*

interface UserLookupContract {
    fun findById(id: UUID): UserResult
    fun findByEmailOrNull(email: String): UserResult?
    fun findAllByIds(ids: Collection<UUID>): List<UserResult>
}
