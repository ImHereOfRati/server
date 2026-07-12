package com.kdongsu5509.user.service

import com.kdongsu5509.user.service.dto.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

/**
 * 일반 사용자(본인) 유스케이스 역할 인터페이스 (ISP / 진단 U1).
 * self-service 컨트롤러(UserReadController/UserCommandController)가 이 역할에만 의존한다.
 */
interface UserSelfService {
    fun findByEmail(email: String): UserResult
    fun findByKeyword(email: String, keyword: String, pageable: Pageable): Slice<UserResult>
    fun updateNickname(userEmail: String, newNickname: String): UserResult
    fun withdraw(userEmail: String): UserResult
}
