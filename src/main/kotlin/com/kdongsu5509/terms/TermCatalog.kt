package com.kdongsu5509.terms

data class TermFact(
    val id: Long,
    val version: Long,
    val type: String,
    val isRequired: Boolean,
)

interface TermCatalog {
    fun findEffectiveTermFacts(): List<TermFact>
    fun findTermFacts(ids: Set<Long>): List<TermFact>
}
