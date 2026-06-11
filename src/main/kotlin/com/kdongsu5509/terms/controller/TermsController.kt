package com.kdongsu5509.terms.controller

import com.kdongsu5509.terms.controller.dto.TermResponse
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/terms", version = "1")
class TermsController(
    private val termService: TermService,
) {
    @GetMapping(params = ["isActive"])
    fun readAllByActive(@RequestParam isActive: Boolean): List<TermResponse> {
        val results = termService.findAll(isActive)
        return convertToTermResponses(results)
    }

    private fun convertToTermResponses(terms: List<TermResult>): List<TermResponse> {
        return terms.map { TermResponse.from(it) }
    }
}
