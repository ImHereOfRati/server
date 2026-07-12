package com.kdongsu5509.terms.service

import com.kdongsu5509.terms.domain.Term
import com.kdongsu5509.terms.domain.TermTypes

/**
 * terms 조회/저장 추상(포트). 도메인/서비스 계층이 소유하고 repository 어댑터가 구현한다.
 * 상위(service)와 타 모듈(user)은 이 추상에만 의존해 영속 세부 결합을 끊는다(DIP).
 */
interface TermRepository {
    fun save(term: Term): Term
    fun findLatestByType(type: TermTypes): Term?
    fun findAll(): List<Term>
    fun findActiveAll(): List<Term>
    fun findById(id: Long): Term?
}
