package com.kdongsu5509.maps

import com.kdongsu5509.shared.response.ApiResponse
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/maps")
class NaverMapProxyController(
    private val service: NaverMapProxyService,
) {
    @GetMapping("/geocode")
    fun geocode(
        @RequestParam @Size(min = 1, max = 200) query: String,
    ): ApiResponse<Map<String, Any?>> =
        ApiResponse.success(service.geocode(query))

    @GetMapping("/reverse-geocode")
    fun reverseGeocode(
        @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") latitude: Double,
        @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") longitude: Double,
    ): ApiResponse<Map<String, Any?>> =
        ApiResponse.success(service.reverseGeocode(latitude, longitude))

    @GetMapping("/local-search")
    fun localSearch(
        @RequestParam @Size(min = 1, max = 100) query: String,
        @RequestParam(defaultValue = "5") @Min(1) @Max(10) display: Int,
    ): ApiResponse<Map<String, Any?>> =
        ApiResponse.success(service.searchLocal(query, display))

    @ExceptionHandler(NaverMapProxyException::class)
    fun handleProxyException(
        error: NaverMapProxyException,
    ): ResponseEntity<ApiResponse<Unit>> =
        ResponseEntity.status(error.status)
            .body(ApiResponse.fail(error.code, error.message))
}
