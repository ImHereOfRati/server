package com.kdongsu5509.user.repository

import com.kdongsu5509.shared.BaseTimeEntity
import com.kdongsu5509.terms.adapter.out.TermJpaEntity
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(name = "user_agreement")
class UserAgreementJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserJpaEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_version_id", nullable = false)
    val term: TermJpaEntity
) : BaseTimeEntity() {
    @Id
    @GeneratedValue
    @UuidGenerator
    val id: UUID? = null
}
