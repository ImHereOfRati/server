package com.kdongsu5509.auth.security

import com.kdongsu5509.support.security.AllowPendingUser
import com.kdongsu5509.support.logger.logger
import org.aopalliance.intercept.MethodInvocation
import org.springframework.aop.support.AopUtils
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.util.ClassUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.function.Supplier

class ActiveUserAuthorizationManager(
    permitAllPaths: List<String>,
) : AuthorizationManager<MethodInvocation> {
    private val log = logger()

    private val permitAllMatchers = permitAllPaths.map(PathPatternRequestMatcher::pathPattern)
    private val pendingUserAllowedMatchers = listOf(
        PathPatternRequestMatcher.pathPattern("/api/users/my/withdrawal"),
    )

    override fun authorize(
        authentication: Supplier<out Authentication>,
        invocation: MethodInvocation,
    ): AuthorizationDecision {
        if (isPendingUserAllowed(invocation) || isPendingUserWithdrawal() || isPublicRequest()) {
            return AuthorizationDecision(true)
        }

        val currentAuthentication = authentication.get()
        val authorities = currentAuthentication.authorities.mapTo(mutableSetOf()) { it.authority }
        val isAllowed = currentAuthentication.isAuthenticated &&
                (UserStatusAuthority.ACTIVE in authorities || "ROLE_ADMIN" in authorities)

        if (!isAllowed) {
            log.debug("활성 사용자 권한 거부: authorities={}, method={}", authorities, invocation.method.name)
        }
        return AuthorizationDecision(isAllowed)
    }

    private fun isPendingUserAllowed(invocation: MethodInvocation): Boolean {
        val targetClass = invocation.`this`
            ?.let { ClassUtils.getUserClass(it) }
            ?: invocation.method.declaringClass
        val method = AopUtils.getMostSpecificMethod(invocation.method, targetClass)

        return AnnotatedElementUtils.hasAnnotation(method, AllowPendingUser::class.java) ||
                AnnotatedElementUtils.hasAnnotation(targetClass, AllowPendingUser::class.java)
    }

    private fun isPublicRequest(): Boolean {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
            ?.request
            ?: return false

        return permitAllMatchers.any { it.matches(request) }
    }

    private fun isPendingUserWithdrawal(): Boolean {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
            ?.request
            ?: return false

        return pendingUserAllowedMatchers.any { it.matches(request) }
    }
}
