package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.application.port.out.CheckUserPort
import org.springframework.stereotype.Component

@Component
class UserCheckPersistenceAdapter(
    private val springDataUserRepository: SpringDataUserRepository,

    ) : CheckUserPort {
    override fun existsByEmail(email: String): Boolean {
        return springDataUserRepository.existsByEmail(email)
    }
}