package com.kdongsu5509.user.service

import com.kdongsu5509.user.service.dto.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface UserService {
    fun findById(id: UUID): UserResult
    fun findByEmail(email: String): UserResult
    fun findAll(pageable: Pageable): Slice<UserResult>
    fun findByKeyword(
        email: String,
        keyword: String,
        pageable: Pageable
    ): Slice<UserResult>

    fun updateNickname(userEmail: String, newNickname: String): UserResult
    fun block(userEmail: String): UserResult
    fun unblock(userEmail: String): UserResult
    fun withdraw(userEmail: String): UserResult
}
