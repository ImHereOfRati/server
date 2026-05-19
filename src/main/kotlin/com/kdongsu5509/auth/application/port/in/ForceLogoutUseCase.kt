package com.kdongsu5509.auth.application.port.`in`

/**
 * TODO : 강제 로그아웃 기능은 제거해도 된다. -> 대신 회원 탈퇴 플로우는 필요한 듯...?
 */
interface ForceLogoutUseCase {
    fun logout(userEmail: String)
}
