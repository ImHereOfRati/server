package com.kdongsu5509.support.exception

open class BusinessException(
    val errorCode: BaseErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message)
