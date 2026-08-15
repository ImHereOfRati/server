package com.kdongsu5509.admin

import com.kdongsu5509.auth.application.service.AdminAuthService
import com.kdongsu5509.auth.application.service.AdminLoginBlockedException
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.service.FriendRelationAdminCommandService
import com.kdongsu5509.friends.service.FriendRelationAdminQueryService
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.terms.service.TermCreateCommand
import com.kdongsu5509.terms.service.TermService
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.user.service.UserLifecycleService
import com.kdongsu5509.user.service.UserQueryService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.UUID

@Controller
@RequestMapping("/admin")
class AdminWebController(
    private val authService: AdminAuthService,
    private val userQueryService: UserQueryService,
    private val userLifecycleService: UserLifecycleService,
    private val relationQueryService: FriendRelationAdminQueryService,
    private val relationCommandService: FriendRelationAdminCommandService,
    private val termService: TermService,
    private val notificationService: NotificationUseCase,
    private val operationalStatus: AdminOperationalStatus,
    @Value("\${admin.id}") private val adminId: String,
    @Value("\${admin.nickname:rati}") private val nickname: String,
) {
    private val securityContextRepository = HttpSessionSecurityContextRepository()

    @GetMapping("/login")
    fun loginPage(session: HttpSession, model: Model): String {
        model.addAttribute("mfaRequired", session.getAttribute(CHALLENGE_KEY) != null)
        model.addAttribute("error", session.getAttribute(ERROR_KEY))
        session.removeAttribute(ERROR_KEY)
        return "admin/login"
    }

    @PostMapping("/login")
    fun login(
        @RequestParam adminId: String,
        @RequestParam password: String,
        request: HttpServletRequest,
        session: HttpSession,
    ): String {
        return try {
            val challenge = authService.begin(adminId, password, resolveClientIp(request)).challenge
            session.setAttribute(CHALLENGE_KEY, challenge)
            "redirect:/admin/login"
        } catch (exception: AdminLoginBlockedException) {
            session.setAttribute(ERROR_KEY, "로그인 시도가 너무 많습니다. 7일 후 다시 시도해 주세요.")
            "redirect:/admin/login"
        } catch (_: Exception) {
            session.setAttribute(ERROR_KEY, "관리자 ID 또는 비밀번호를 확인해 주세요.")
            "redirect:/admin/login"
        }
    }

    @PostMapping("/login/mfa")
    fun verifyMfa(
        @RequestParam code: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
        session: HttpSession,
    ): String {
        val challenge = session.getAttribute(CHALLENGE_KEY) as? String
        if (challenge == null) {
            session.setAttribute(ERROR_KEY, "로그인 절차가 만료되었습니다.")
            return "redirect:/admin/login"
        }
        return try {
            authService.verify(challenge, code)
            val principal = ImHereUserDetails(
                email = adminId,
                nickname = nickname,
                role = "ADMIN",
                status = "ACTIVE",
                userId = UUID.nameUUIDFromBytes("imhere-admin:$adminId".toByteArray()),
            )
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.authorities,
            )
            SecurityContextHolder.setContext(context)
            securityContextRepository.saveContext(context, request, response)
            session.removeAttribute(CHALLENGE_KEY)
            "redirect:/admin"
        } catch (_: Exception) {
            session.setAttribute(ERROR_KEY, "인증 코드가 올바르지 않거나 만료되었습니다.")
            "redirect:/admin/login"
        }
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): String {
        SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().authentication)
        return "redirect:/admin/login"
    }

    @GetMapping
    fun dashboard(model: Model): String {
        model.addAttribute("page", "dashboard")
        model.addAttribute("adminId", adminId)
        model.addAttribute("checkedAt", LocalDateTime.now())
        model.addAttribute("health", operationalStatus.health())
        model.addAttribute("info", operationalStatus.info())
        return "admin/dashboard"
    }

    @GetMapping("/users")
    fun users(@RequestParam(defaultValue = "0") page: Int, model: Model): String {
        model.addAttribute("users", userQueryService.findAll(PageRequest.of(page, 15, Sort.by("createdAt").descending())))
        model.addAttribute("pageNumber", page)
        return "admin/users"
    }

    @PostMapping("/users/{email}/block")
    fun block(@PathVariable email: String): String { userLifecycleService.block(email); return "redirect:/admin/users" }

    @PostMapping("/users/{email}/unblock")
    fun unblock(@PathVariable email: String): String { userLifecycleService.unblock(email); return "redirect:/admin/users" }

    @PostMapping("/users/{email}/force-logout")
    fun forceLogout(@PathVariable email: String): String { userLifecycleService.requestForceLogout(email); return "redirect:/admin/users" }

    @PostMapping("/users/{email}/withdraw")
    fun withdraw(@PathVariable email: String): String { userLifecycleService.withdraw(email); return "redirect:/admin/users" }

    @GetMapping("/relations/{kind}")
    fun relations(@PathVariable kind: String, @RequestParam(defaultValue = "0") page: Int, model: Model): String {
        val pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending())
        val items = when (kind) {
            "requests" -> relationQueryService.findAllRequests(pageable)
            "friendships" -> relationQueryService.findAllFriendships(pageable)
            "restrictions" -> relationQueryService.findAllRestrictions(pageable)
            else -> throw IllegalArgumentException("Unknown relation kind")
        }
        model.addAttribute("kind", kind)
        model.addAttribute("items", items)
        model.addAttribute("pageNumber", page)
        return "admin/relations"
    }

    @PostMapping("/relations/{kind}/{id}/delete")
    fun deleteRelation(@PathVariable kind: String, @PathVariable id: UUID): String {
        relationCommandService.deleteById(id)
        return "redirect:/admin/relations/$kind"
    }

    @GetMapping("/terms")
    fun terms(model: Model): String { model.addAttribute("terms", termService.findAll()); return "admin/terms" }

    @PostMapping("/terms")
    fun createTerm(
        @RequestParam type: String,
        @RequestParam title: String,
        @RequestParam content: String,
        @RequestParam(defaultValue = "false") isRequired: Boolean,
        @RequestParam effectiveDate: String,
    ): String {
        termService.save(TermCreateCommand(TermTypes.valueOf(type), title, content, LocalDateTime.parse(effectiveDate), isRequired))
        return "redirect:/admin/terms"
    }

    @GetMapping("/failed-notifications")
    fun failedNotifications(@RequestParam(defaultValue = "0") page: Int, model: Model): String {
        model.addAttribute("notifications", notificationService.findAll(NotificationStatus.DEAD, page, 20))
        model.addAttribute("pageNumber", page)
        return "admin/failed-notifications"
    }

    @PostMapping("/failed-notifications/{id}/redeliver")
    fun redeliver(@PathVariable id: Long): String { notificationService.redeliver(id); return "redirect:/admin/failed-notifications" }

    @PostMapping("/failed-notifications/redeliver")
    fun redeliverAll(): String { notificationService.redeliverAll(null); return "redirect:/admin/failed-notifications" }

    @PostMapping("/failed-notifications/{id}/discard")
    fun discard(@PathVariable id: Long): String { notificationService.discard(id); return "redirect:/admin/failed-notifications" }

    companion object {
        const val CHALLENGE_KEY = "admin.mfa.challenge"
        const val ERROR_KEY = "admin.login.error"
    }

    private fun resolveClientIp(request: HttpServletRequest): String =
        request.getHeader("X-Real-IP")?.takeIf { it.isNotBlank() }
            ?: request.getHeader("X-Forwarded-For")?.split(",")?.lastOrNull()?.trim()?.takeIf { !it.isNullOrBlank() }
            ?: request.remoteAddr
}
