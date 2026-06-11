package com.kdongsu5509.terms.controller

import com.kdongsu5509.terms.service.TermService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin/terms")
class TermsAdminPageController(
    private val termService: TermService
) {
    @GetMapping
    fun page(model: Model): String {
        model.addAttribute("terms", termService.findAll())
        return "admin/terms"
    }
}
