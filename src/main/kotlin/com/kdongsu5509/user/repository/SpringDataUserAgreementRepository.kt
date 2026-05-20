package com.kdongsu5509.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SpringDataUserAgreementRepository : JpaRepository<UserAgreementJpaEntity, UUID>
