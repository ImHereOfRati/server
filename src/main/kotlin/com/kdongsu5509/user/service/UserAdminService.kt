package com.kdongsu5509.user.service

import com.kdongsu5509.user.service.dto.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

/**
 * 관리자 유스케이스 역할 인터페이스 (ISP / 진단 U1).
 * admin 컨트롤러(UserAdminController/UserAdminPageController)가 이 역할에만 의존한다.
 */
interface UserAdminService {
    fun findAll(pageable: Pageable): Slice<UserResult>
    fun block(userEmail: String): UserResult
    fun unblock(userEmail: String): UserResult
    fun withdraw(userEmail: String): UserResult
}
