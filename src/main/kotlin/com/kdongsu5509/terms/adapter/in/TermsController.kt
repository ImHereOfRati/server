package com.kdongsu5509.terms.adapter.`in`

import com.kdongsu5509.terms.adapter.`in`.dto.TermCreateRequest
import com.kdongsu5509.terms.adapter.`in`.dto.TermResponse
import com.kdongsu5509.terms.application.TermCreateCommand
import com.kdongsu5509.terms.application.TermResult
import com.kdongsu5509.terms.application.TermService
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
        return TermResponse.from(result)
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
        return terms.map { TermResponse.from(it) }
    }
}
