package com.kdongsu5509.user.application.service.user

import com.kdongsu5509.user.domain.user.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class SimpleTokenUserDetails(
    private val email: String,
    val nickname: String,
    val role: String,
    val status: String
) : UserDetails {

    private val authoritiesList: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_$role"))

    override fun getAuthorities(): Collection<GrantedAuthority> = authoritiesList

    override fun getUsername(): String = email
    override fun getPassword(): String? = null
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = (status != UserStatus.BLOCKED.name)
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = (status != UserStatus.BLOCKED.name)
}