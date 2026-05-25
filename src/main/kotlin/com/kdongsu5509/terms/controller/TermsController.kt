package com.kdongsu5509.terms.controller

import com.kdongsu5509.terms.controller.dto.TermCreateRequest
import com.kdongsu5509.terms.controller.dto.TermResponse
import com.kdongsu5509.terms.service.TermCreateCommand
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/terms", version = "1")
class TermsController(
    private val termService: TermService,
) {
    @PostMapping
    fun create(@RequestBody @Validated request: TermCreateRequest): TermResponse {
        val result = termService.save(TermCreateCommand.fromRequest(request))
        return TermResponse.Companion.from(result)
    }

    @GetMapping(params = ["isActive"])
    fun readAllByActive(@RequestParam isActive: Boolean): List<TermResponse> {
        val results = termService.findAll(isActive)
        return convertToTermResponses(results)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun readAll(): List<TermResponse> {
        val results = termService.findAll()
        return convertToTermResponses(results)
    }

    private fun convertToTermResponses(terms: List<TermResult>): List<TermResponse> {
        return terms.map { TermResponse.Companion.from(it) }
    }
}
