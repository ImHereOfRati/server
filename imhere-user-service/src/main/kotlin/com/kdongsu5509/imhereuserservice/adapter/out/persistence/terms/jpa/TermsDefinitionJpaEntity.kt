package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.common.BaseEntity
import com.kdongsu5509.imhereuserservice.domain.terms.TermsTypes
import jakarta.persistence.*

@Entity
@Table(name = "terms")
class TermsDefinitionJpaEntity(
    @Column(nullable = false)
    var termsTitle: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var termsType: TermsTypes,

    @Column(nullable = false)
    var isRequired: Boolean
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "terms", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val versions: MutableList<TermsVersionJpaEntity> = mutableListOf()
}