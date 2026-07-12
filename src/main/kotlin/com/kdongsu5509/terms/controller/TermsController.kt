package com.kdongsu5509.terms.controller

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.controller.dto.TermResponse
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
        if (!isActive) {
            TermException.NON_ACTIVE_TERM_NOT_ALLOWED.throwIt()
        }
        return TermResponse.listFrom(termService.findEffectiveTerms())
    }
}
