package com.kdongsu5509.auth.security.shared

import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@PreAuthorize("hasAnyAuthority('STATUS_ACTIVE', 'STATUS_PENDING')")
annotation class AllowPendingUser
