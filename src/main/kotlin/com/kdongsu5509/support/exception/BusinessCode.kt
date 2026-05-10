package com.kdongsu5509.support.exception

import com.kdongsu5509.support.exception.type.*

/**
 * 도메인별 세부 비즈니스 에러 코드를 정의하기 위한 인터페이스
 */
interface BusinessCode {
    val errorCategory: ErrorReason
    val businessCode: String
    val message: String? // 필수에서 선택으로 변경하여 ErrorReason의 기본 메시지를 활용할 수 있게 함
}

/**
 * [BusinessCode]를 바탕으로 적절한 시맨틱 예외를 던집니다.
 *
 * @param metadata 예외와 함께 전달할 상세 컨텍스트 데이터
 * @param customMessage 기본 정의된 메시지 대신 사용할 커스텀 메시지
 * @param cause 원인이 된 예외
 * @throws BaseException 매핑된 시맨틱 예외 객체
 */
fun BusinessCode.throwIt(
    metadata: Map<String, Any?> = emptyMap(),
    customMessage: String? = null,
    cause: Throwable? = null
): Nothing {
    val contextData = metadata.toMutableMap().apply {
        put("businessCode", businessCode)
    }

    // 우선순위: customMessage > Enum의 message > ErrorReason의 defaultMessage
    val finalMessage = customMessage ?: this.message ?: this.errorCategory.defaultMessage

    throw when (this.errorCategory) {
        ErrorReason.INVALID_INPUT -> InvalidInputException(finalMessage, contextData, cause)
        ErrorReason.UNAUTHORIZED -> UnauthorizedException(finalMessage, contextData, cause)
        ErrorReason.FORBIDDEN -> ForbiddenException(finalMessage, contextData, cause)
        ErrorReason.NOT_FOUND -> NotFoundException(finalMessage, contextData, cause)
        ErrorReason.CONFLICT -> ConflictException(finalMessage, contextData, cause)
        ErrorReason.INFRA_FAILURE -> InfraFailureException(finalMessage, contextData, cause)
        ErrorReason.INTERNAL_SERVER_ERROR -> InternalServerException(finalMessage, contextData, cause)
    }
}
