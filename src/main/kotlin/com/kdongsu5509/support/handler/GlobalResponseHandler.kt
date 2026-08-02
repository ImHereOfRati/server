package com.kdongsu5509.support.handler

import com.kdongsu5509.shared.response.ApiResponse
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@RestControllerAdvice(basePackages = ["com.kdongsu5509"])
class GlobalResponseHandler : ResponseBodyAdvice<Any> {

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>
    ): Boolean {
        val returnClass = returnType.parameterType

        if (ApiResponse::class.java.isAssignableFrom(returnClass)) return false

        if (ResponseEntity::class.java.isAssignableFrom(returnClass)) {
            val typeName = returnType.genericParameterType.typeName
            if (typeName.contains("ApiResponse")) return false
        }

        val declaringClass = returnType.declaringClass.name
        if (declaringClass.contains("springdoc") || declaringClass.contains("swagger")) return false

        return true
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        if (body is ApiResponse<*>) {
            return body
        }

        return ApiResponse.success(body)
    }
}
