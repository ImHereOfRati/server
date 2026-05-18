package com.kdongsu5509.auth.security

import com.kdongsu5509.auth.domain.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class SimpleTokenUserDetails(
    val email: String,
    val nickname: String,
    val role: String,
    val status: String
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_$role"))
    }

    override fun getPassword(): String? = null

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean {
        return status != UserStatus.BLOCKED.name
    }

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean {
        return status == UserStatus.ACTIVE.name && status != UserStatus.PENDING.name
    }
}
