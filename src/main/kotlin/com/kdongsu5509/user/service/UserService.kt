package com.kdongsu5509.user.service

import com.kdongsu5509.user.service.dto.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface UserService {
    fun findByEmail(email: String): UserResult
    fun findAll(pageable: Pageable): Slice<UserResult>
    fun findByKeyword(
        email: String,
        keyword: String,
        pageable: Pageable
    ): Slice<UserResult>

    fun updateNickname(userEmail: String, newNickname: String): UserResult
    fun block(userEmail: String)
    fun unblock(userEmail: String)
}
