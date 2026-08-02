package com.kdongsu5509.auth.domain

object RoleAuthority {

    private const val AUTHORITY_PREFIX = "ROLE_"

    fun toAuthority(role: String): String = "$AUTHORITY_PREFIX$role"

    fun fromAuthority(authority: String): String = authority.removePrefix(AUTHORITY_PREFIX)
}
