package com.kdongsu5509.user.api

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*
import com.kdongsu5509.user.domain.OAuth2Provider

interface UserLookupContract {
    fun findById(id: UUID): UserResult
    fun findByEmailOrNull(email: String): UserResult?
    fun findByOidcIdentityOrNull(provider: OAuth2Provider, oidcSubject: String): UserResult?
    fun findAllByIds(ids: Collection<UUID>): List<UserResult>

    fun searchActiveByKeyword(
        keyword: String,
        excludedUserIds: Set<UUID>,
        pageable: Pageable,
    ): Slice<UserResult>
}
