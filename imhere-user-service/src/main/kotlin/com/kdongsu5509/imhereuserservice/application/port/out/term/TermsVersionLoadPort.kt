package com.kdongsu5509.imhereuserservice.application.port.out.term

import com.kdongsu5509.imhereuserservice.domain.terms.TermVersion

interface TermsVersionLoadPort {
    fun loadSpecificActiveTermVersion(termDefinitionId: Long): TermVersion
}