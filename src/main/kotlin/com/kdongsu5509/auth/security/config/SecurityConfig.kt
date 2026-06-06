package com.kdongsu5509.auth.security.config

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.auth.security.filter.AdminSecretFilter
import com.kdongsu5509.auth.security.filter.JwtAuthenticationFilter
import com.kdongsu5509.auth.security.handler.ImHereOttSuccessHandler
import com.kdongsu5509.auth.security.handler.OttLoginSuccessHandler
import com.kdongsu5509.shared.response.APIResponseSerializers
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.security.authentication.ott.JdbcOneTimeTokenService
import org.springframework.security.authentication.ott.OneTimeTokenService
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.ott.GenerateOneTimeTokenFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true
)
@EnableConfigurationProperties(SecurityWhiteList::class)
class SecurityConfig(
    private val securityWhiteList: SecurityWhiteList,
    private val imHereJwtTokenParserPort: ImHereTokenParserPort,
    private val imHereOttSuccessHandler: ImHereOttSuccessHandler,
    private val ottLoginSuccessHandler: OttLoginSuccessHandler,
    @param:Value("\${admin.secret}") private val adminSecret: String,
    @param:Value("\${admin.id}") private val adminId: String,
) {

    @Bean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter =
        JwtAuthenticationFilter(imHereJwtTokenParserPort, securityWhiteList)

    @Bean
    fun userDetailsService(): UserDetailsService = UserDetailsService { email ->
        if (email == adminId) {
            User.withUsername(adminId)
                .password("{noop}N/A")
                .roles(UserRole.ADMIN.name)
                .build()
        } else {
            throw UsernameNotFoundException("관리자 계정을 찾을 수 없습니다: $email")
        }
    }

    @Bean
    fun oneTimeTokenService(jdbcOperation: JdbcOperations): OneTimeTokenService =
        JdbcOneTimeTokenService(jdbcOperation)

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }

            headers {
                frameOptions { sameOrigin }
            }

            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            authorizeHttpRequests {
                securityWhiteList.whitelist.forEach { authorize(it, permitAll) }
                authorize("/api/admin/**", hasRole(UserRole.ADMIN.name))
                authorize(anyRequest, authenticated)
            }

            exceptionHandling {
                authenticationEntryPoint = { _, response, _ ->
                    APIResponseSerializers.writeErrorResponse(
                        response = response,
                        status = HttpStatus.UNAUTHORIZED,
                        imhereErrorCode = AuthException.IMHERE_INVALID_TOKEN.imhereErrorCode,
                        errorMessage = "인증이 필요합니다."
                    )
                }
                accessDeniedHandler = { _, response, _ ->
                    APIResponseSerializers.writeErrorResponse(
                        response = response,
                        status = HttpStatus.FORBIDDEN,
                        imhereErrorCode = AuthException.IMHERE_ACCESS_DENIED.imhereErrorCode,
                        errorMessage = "접근 권한이 없습니다."
                    )
                }
            }

            oneTimeTokenLogin {
                showDefaultSubmitPage = false
                tokenGeneratingUrl = "/api/admin/auth/ott"
                loginProcessingUrl = "/api/admin/auth"
                oneTimeTokenGenerationSuccessHandler = imHereOttSuccessHandler // 1. 발급 성공 시 메신저 발송
                authenticationSuccessHandler = ottLoginSuccessHandler // 2. 인증(OTP) 성공 시 처리 (JWT 발급 등)
                authenticationFailureHandler = { _, response, _ ->
                    val error = AuthException.INVALID_OTT
                    APIResponseSerializers.writeErrorResponse(
                        response = response,
                        status = error.httpStatus,
                        imhereErrorCode = error.imhereErrorCode,
                        errorMessage = error.errorMessage
                    )
                }
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter())
            addFilterBefore<GenerateOneTimeTokenFilter>(AdminSecretFilter(adminSecret))
        }

        return http.build()
    }
}
