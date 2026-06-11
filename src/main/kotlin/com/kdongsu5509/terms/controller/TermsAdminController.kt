package com.kdongsu5509.terms.controller

import com.kdongsu5509.terms.controller.dto.TermCreateRequest
import com.kdongsu5509.terms.controller.dto.TermResponse
import com.kdongsu5509.terms.service.TermCreateCommand
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/terms", version = "1")
class TermsAdminController(
    private val termService: TermService,
) {
    @GetMapping
    fun readAll(): List<TermResponse> {
        val results = termService.findAll()
        return convertToTermResponses(results)
    }

    @PostMapping
    fun create(@RequestBody @Validated request: TermCreateRequest): TermResponse {
        val result = termService.save(TermCreateCommand.fromRequest(request))
        return TermResponse.from(result)
    }

    private fun convertToTermResponses(terms: List<TermResult>): List<TermResponse> {
        return terms.map { TermResponse.from(it) }
    }
}
