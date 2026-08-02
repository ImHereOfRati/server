package com.kdongsu5509.user.api

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface UserLookupContract {
    fun findById(id: UUID): UserResult
    fun findByEmailOrNull(email: String): UserResult?
    fun findAllByIds(ids: Collection<UUID>): List<UserResult>

    fun searchActiveByKeyword(
        keyword: String,
        excludedUserIds: Set<UUID>,
        pageable: Pageable,
    ): Slice<UserResult>
}
