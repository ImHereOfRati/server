package com.kdongsu5509.support.exception

/**
 * Loki 친화적 장애 진단을 위한 최소형 기본 예외 클래스
 *
 * @property errorCategory 예외의 종류 (HTTP 상태 코드 및 GLOBAL-XXX 코드 정의)
 * @property message 에러 상세 메시지
 * @property metadata 에러와 관련된 추가 컨텍스트 데이터 (비즈니스 에러 코드 등)
 * @property cause 원인이 된 예외 (Stack Trace 보존용)
 */
abstract class BaseException(
    val errorCategory: ErrorReason,
    override val message: String,
    val metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : RuntimeException(message, cause)
