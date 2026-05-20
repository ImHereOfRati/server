package com.kdongsu5509.user.repository

import java.util.UUID

interface UserAgreementDao {
    fun save(userId: UUID, id: Long)
    fun saveAll(userId: UUID, ids: List<Long>)
}
