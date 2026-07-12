package com.kdongsu5509.friends.domain

data class RequestMessage(val value: String) {
    init {
        require(value.isNotBlank()) { "친구 요청 메시지는 비어있을 수 없습니다." }
    }
}
