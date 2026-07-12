package com.kdongsu5509.terms.controller

import com.kdongsu5509.terms.controller.dto.TermCreateRequest
import com.kdongsu5509.terms.controller.dto.TermResponse
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
        return TermResponse.listFrom(termService.findAll())
    }

    @PostMapping
    fun create(@RequestBody @Validated request: TermCreateRequest): TermResponse {
        val result = termService.save(request.toCommand())
        return TermResponse.from(result)
    }
}
