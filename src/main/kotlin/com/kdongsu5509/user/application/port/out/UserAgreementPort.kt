package com.kdongsu5509.user.application.port.out

interface UserAgreementPort {
    fun save(email: String, id: Long)
    fun saveAll(email: String, ids: List<Long>)
}
