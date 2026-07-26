package com.kdongsu5509.auth.security

import com.kdongsu5509.auth.security.shared.AllowPendingUser
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

    private val permitAllMatchers = permitAllPaths.map(PathPatternRequestMatcher::pathPattern)

    override fun authorize(
        authentication: Supplier<out Authentication>,
        invocation: MethodInvocation,
    ): AuthorizationDecision {
        if (isPendingUserAllowed(invocation) || isPublicRequest()) {
            return AuthorizationDecision(true)
        }

        val currentAuthentication = authentication.get()
        val authorities = currentAuthentication.authorities.mapTo(mutableSetOf()) { it.authority }
        val isAllowed = currentAuthentication.isAuthenticated &&
                (UserStatusAuthority.ACTIVE in authorities || "ROLE_ADMIN" in authorities)

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
}
