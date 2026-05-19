package com.kdongsu5509.terms.adapter.out.jpa

import com.kdongsu5509.terms.domain.TermsTypes
import com.kdongsu5509.user.adapter.out.persistence.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "term_policy")
class TermPolicyJpaEntity(
    @Column(nullable = false)
    var title: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: TermsTypes,

    @Column(nullable = false)
    var isRequired: Boolean
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "termPolicy", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val contentJpaEntities: MutableList<TermContentJpaEntity> = mutableListOf()
}
