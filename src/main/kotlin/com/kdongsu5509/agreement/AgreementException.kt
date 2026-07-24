package com.kdongsu5509.agreement

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode

enum class AgreementException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String,
) : ImHereBaseErrorCode {
    REQUIRED_AGREEMENTS_NOT_SATISFIED(
        CommonErrorCode.INVALID_INPUT,
        "AGREEMENT-000",
        "필수 약관에 동의해야 합니다.",
    ),
    REQUIRED_AGREEMENT_CANNOT_BE_WITHDRAWN(
        CommonErrorCode.UNPROCESSABLE_ENTITY,
        "AGREEMENT-700",
        "필수 약관 동의는 철회할 수 없습니다.",
    );

    override val httpStatus = category.httpStatus
}
