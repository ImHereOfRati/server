package com.kdongsu5509.auth.security

import com.kdongsu5509.auth.domain.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class ImHereUserDetails(
    val email: String,
    val nickname: String,
    val role: String,
    val status: String
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_$role"))

        if (status == UserStatus.PENDING.name) {
            authorities.add(SimpleGrantedAuthority(UserStatus.PENDING.name))
        }

        return authorities
    }

    override fun getPassword(): String? = null

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean {
        return status != UserStatus.BLOCKED.name
    }

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean {
        return status == UserStatus.ACTIVE.name || status == UserStatus.PENDING.name
    }
}
