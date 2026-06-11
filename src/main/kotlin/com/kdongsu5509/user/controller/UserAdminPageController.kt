package com.kdongsu5509.user.controller

import com.kdongsu5509.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin/users")
class UserAdminPageController(
    private val userService: UserService
) {
    @GetMapping
    fun page(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val result = userService.findAll(PageRequest.of(page, size))
        model.addAttribute("users", result.content)
        model.addAttribute("hasNext", result.hasNext())
        return "admin/users"
    }
}
