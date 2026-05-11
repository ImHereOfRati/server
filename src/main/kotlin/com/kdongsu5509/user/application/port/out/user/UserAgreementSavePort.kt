package com.kdongsu5509.user.application.port.out.user

interface UserAgreementSavePort {
    fun save(userEmail: String, termDefinitionId: Long)
    fun saveAll(userEmail: String, termDefinitionIds: List<Long>)
}
