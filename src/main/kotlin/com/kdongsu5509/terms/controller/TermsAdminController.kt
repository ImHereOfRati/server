package com.kdongsu5509.terms.controller

import com.kdongsu5509.terms.controller.dto.TermResponse
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/terms", version = "1")
class TermsAdminController(
    private val termService: TermService,
) {
    @GetMapping
    fun readAll(): List<TermResponse> {
        val results = termService.findAll()
        return convertToTermResponses(results)
    }

    private fun convertToTermResponses(terms: List<TermResult>): List<TermResponse> {
        return terms.map { TermResponse.from(it) }
    }
}
