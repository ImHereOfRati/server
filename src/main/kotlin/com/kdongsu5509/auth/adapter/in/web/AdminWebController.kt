package com.kdongsu5509.auth.adapter.`in`.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class AdminWebController(
    @param:Value("\${admin.id}") private val adminId: String
) {
    @GetMapping("/admin/login")
    fun loginPage(model: Model): String {
        model.addAttribute("adminId", adminId)
        return "admin/login"
    }

    @GetMapping("/admin/ott")
    fun ottPage(
        @RequestParam(required = false) username: String?,
        @RequestParam(required = false, defaultValue = "false") error: Boolean,
        model: Model
    ): String {
        model.addAttribute("username", username ?: adminId)
        model.addAttribute("hasError", error)
        return "admin/ott"
    }

    @GetMapping("/admin", "/admin/")
    fun dashboard(): String = "admin/dashboard"
}
