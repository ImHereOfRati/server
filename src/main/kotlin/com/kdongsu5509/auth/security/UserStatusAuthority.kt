package com.kdongsu5509.auth.security

object UserStatusAuthority {
    private const val PREFIX = "STATUS_"
    const val ACTIVE = "${PREFIX}ACTIVE"
    const val PENDING = "${PREFIX}PENDING"

    fun from(status: String): String = "$PREFIX$status"
}
