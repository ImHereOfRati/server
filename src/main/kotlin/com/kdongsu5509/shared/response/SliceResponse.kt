package com.kdongsu5509.shared.response

import org.springframework.data.domain.Slice

data class SliceResponse<T : Any>(
    val content: List<T>,
    val hasNext: Boolean
) {
    companion object {
        fun <T : Any> from(slice: Slice<T>): SliceResponse<T> {
            return SliceResponse(
                content = slice.content,
                hasNext = slice.hasNext()
            )
        }
    }
}
