package com.kdongsu5509.agreement

import com.kdongsu5509.shared.exception.ImHereBaseErrorCode
import org.springframework.http.HttpStatus

enum class AgreementException(
    override val httpStatus: HttpStatus,
    override val imhereErrorCode: String,
    override val errorMessage: String,
) : ImHereBaseErrorCode {
    REQUIRED_AGREEMENTS_NOT_SATISFIED(
        HttpStatus.BAD_REQUEST,
        "AGREEMENT-000",
        "필수 약관에 동의해야 합니다.",
    ),
    REQUIRED_AGREEMENT_CANNOT_BE_WITHDRAWN(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "AGREEMENT-700",
        "필수 약관 동의는 철회할 수 없습니다.",
    );
}
