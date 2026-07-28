package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.service.FriendRelationAdminCommandService
import com.kdongsu5509.friends.service.FriendRelationAdminQueryService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.*

/**
 * 관리자 관계 화면.
 *
 * 세 페이지 컨트롤러가 서비스만 다르고 구조가 같았다.
 * 경로·모델 속성 이름·템플릿 이름은 화면 계약이라 그대로 둔다.
 */
@Controller
class FriendRelationAdminPageController(
    private val friendRelationAdminQueryService: FriendRelationAdminQueryService,
    private val friendRelationAdminCommandService: FriendRelationAdminCommandService
) {

    @GetMapping("/admin/friend-requests")
    fun requestsPage(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val result = friendRelationAdminQueryService.findAllRequests(PageRequest.of(page, size))
        model.addAttribute("requests", result.content)
        model.addAttribute("hasNext", result.hasNext())
        return "admin/friend-requests"
    }

    @PostMapping("/admin/friend-requests/{id}/delete")
    fun deleteRequest(@PathVariable id: UUID, redirectAttributes: RedirectAttributes): String {
        friendRelationAdminCommandService.deleteById(id)
        redirectAttributes.addFlashAttribute("message", "친구 요청을 삭제했습니다.")
        return "redirect:/admin/friend-requests"
    }

    @GetMapping("/admin/friendships")
    fun friendshipsPage(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val result = friendRelationAdminQueryService.findAllFriendships(PageRequest.of(page, size))
        model.addAttribute("friendships", result.content)
        model.addAttribute("hasNext", result.hasNext())
        return "admin/friendships"
    }

    @PostMapping("/admin/friendships/{id}/delete")
    fun deleteFriendship(@PathVariable id: UUID, redirectAttributes: RedirectAttributes): String {
        friendRelationAdminCommandService.deleteById(id)
        redirectAttributes.addFlashAttribute("message", "친구 관계를 삭제했습니다.")
        return "redirect:/admin/friendships"
    }

    @GetMapping("/admin/friend-restrictions")
    fun restrictionsPage(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val result = friendRelationAdminQueryService.findAllRestrictions(PageRequest.of(page, size))
        model.addAttribute("restrictions", result.content)
        model.addAttribute("hasNext", result.hasNext())
        return "admin/friend-restrictions"
    }

    @PostMapping("/admin/friend-restrictions/{id}/delete")
    fun deleteRestriction(@PathVariable id: UUID, redirectAttributes: RedirectAttributes): String {
        friendRelationAdminCommandService.deleteById(id)
        redirectAttributes.addFlashAttribute("message", "차단/제한 내역을 삭제했습니다.")
        return "redirect:/admin/friend-restrictions"
    }
}
