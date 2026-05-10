package com.kdongsu5509.support.exception

import org.springframework.http.HttpStatus

/**
 * 예외의 종류를 정의하는 열거형 클래스
 * 각 상수는 HTTP 상태 코드, 전역 비즈니스 식별 코드, 그리고 기본 메시지를 가집니다.
 *
 * @property httpStatus 대응되는 HTTP 상태 코드
 * @property globalCode 클라이언트가 식별할 전역 비즈니스 식별 코드 (GLOBAL-XXX)
 * @property defaultMessage 해당 에러 카테고리의 기본 안내 메시지
 */
enum class ErrorReason(
    val httpStatus: HttpStatus,
    val globalCode: String,
    val defaultMessage: String
) {
    /** 입력값 검증 실패 (400) */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "GLOBAL-400", "잘못된 요청입니다. 입력값을 확인해주세요."),

    /** 인증 실패 (401) */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "GLOBAL-401", "인증에 실패했습니다. 다시 로그인해주세요."),

    /** 권한 없음 (403) */
    FORBIDDEN(HttpStatus.FORBIDDEN, "GLOBAL-403", "해당 리소스에 접근할 권한이 없습니다."),

    /** 리소스를 찾을 수 없음 (404) */
    NOT_FOUND(HttpStatus.NOT_FOUND, "GLOBAL-404", "요청하신 리소스를 찾을 수 없습니다."),

    /** 중복 데이터 등 상태 충돌 (409) */
    CONFLICT(HttpStatus.CONFLICT, "GLOBAL-409", "이미 존재하는 데이터이거나 상태가 충돌합니다."),

    /** 외부 인프라 서비스 장애 (500) */
    INFRA_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL-500-INFRA", "외부 서비스와의 통신 중 오류가 발생했습니다."),

    /** 기타 서버 내부 오류 (500) */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL-500", "서버 내부 오류가 발생했습니다.");

    /**
     * 즉각적인 대응이 필요한 크리티컬한 에러인지 여부를 반환합니다.
     * 인프라 장애나 서버 내부 오류는 크리티컬 에러로 간주하여 디스코드 알림 등을 발송합니다.
     */
    fun isCritical(): Boolean = this == INFRA_FAILURE || this == INTERNAL_SERVER_ERROR
}
