package com.kdongsu5509.auth.security.shared

import com.kdongsu5509.auth.domain.RoleAuthority
import com.kdongsu5509.auth.security.UserStatusAuthority
import com.kdongsu5509.user.domain.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

data class ImHereUserDetails(
    val email: String,
    val nickname: String,
    val role: String,
    val status: String,
    val userId: UUID? = null,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(
            SimpleGrantedAuthority(RoleAuthority.toAuthority(role)),
            SimpleGrantedAuthority(UserStatusAuthority.from(status)),
        )
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
