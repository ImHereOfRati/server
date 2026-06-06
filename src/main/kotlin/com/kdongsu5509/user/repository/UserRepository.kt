package com.kdongsu5509.user.repository

import com.kdongsu5509.user.domain.User
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface UserRepository {
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun findAll(pageable: Pageable): Slice<User>
    fun findSliceByEmailAndNickname(
        userEmail: String,
        keyword: String,
        pageable: Pageable
    ): Slice<User>

    fun findActiveUserByEmail(email: String): User?
    fun save(user: User): User
    fun update(user: User)
    fun existsByEmail(email: String): Boolean
}
