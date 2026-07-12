package com.kdongsu5509.user.service

import com.kdongsu5509.user.service.dto.UserResult
import java.util.*

/**
 * 역할 인터페이스(UserSelfService/UserAdminService)를 합성한 사용자 서비스 종합 계약.
 * id 기반 조회(findById)는 타 모듈(friends) 크로스모듈 협력에 필요한 질의 표면으로 여기서 제공한다.
 * 컨트롤러는 각자 필요한 역할 인터페이스만 주입받는다(ISP).
 */
interface UserService : UserSelfService, UserAdminService {
    fun findById(id: UUID): UserResult
}
