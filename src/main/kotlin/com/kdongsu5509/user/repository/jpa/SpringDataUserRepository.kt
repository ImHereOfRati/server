package com.kdongsu5509.user.repository.jpa

import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.domain.OAuth2Provider
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SpringDataUserRepository : JpaRepository<UserJpaEntity, UUID> {
    fun findByEmail(email: String): UserJpaEntity?
    fun findByProviderAndOidcSubject(provider: OAuth2Provider, oidcSubject: String): UserJpaEntity?
    fun findAllByStatusNot(status: UserStatus, pageable: Pageable): Slice<UserJpaEntity>
}
