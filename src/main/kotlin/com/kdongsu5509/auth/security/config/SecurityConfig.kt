package com.kdongsu5509.auth.security.config

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.auth.security.filter.JwtAuthenticationFilter
import com.kdongsu5509.auth.security.filter.OttIpFilterConfig
import com.kdongsu5509.auth.security.filter.OttIpValidationFilter
import com.kdongsu5509.auth.security.handler.ImHereOttSuccessHandler
import com.kdongsu5509.auth.security.handler.OttLoginSuccessHandler
import com.kdongsu5509.shared.response.APIResponseSerializers
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

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
    @Value("\${admin.id}") private val adminId: String,
    @Value("\${admin.allowed-ips:127.0.0.1}") private val allowedIps: List<String>,
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
    fun ottIpFilterConfig(): OttIpFilterConfig {
        return OttIpFilterConfig(adminId, "", allowedIps)
    }

    @Bean
    fun ottIpValidationFilter(config: OttIpFilterConfig): OttIpValidationFilter {
        return OttIpValidationFilter(config)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = securityWhiteList.corsAllowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "X-Requested-With", "X-CSRF-TOKEN")
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuration)
        }
    }

    @Bean
    @Order(1)
    fun adminApiFilterChain(http: HttpSecurity, ottIpValidationFilter: OttIpValidationFilter): SecurityFilterChain {
        http.securityMatcher("/api/admin/**")

        http {
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }
            cors { }

            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.IF_REQUIRED
            }

            authorizeHttpRequests {
                authorize(anyRequest, hasRole(UserRole.ADMIN.name))
            }

            exceptionHandling {
                authenticationEntryPoint = { _, response, _ ->
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized")
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

            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter())
            addFilterBefore<UsernamePasswordAuthenticationFilter>(ottIpValidationFilter)
        }

        return http.build()
    }

    @Bean
    @Order(2)
    fun adminWebFilterChain(http: HttpSecurity, ottIpValidationFilter: OttIpValidationFilter): SecurityFilterChain {
        http.securityMatcher("/admin/**")

        http {
            formLogin { disable() }
            httpBasic { disable() }

            headers {
                frameOptions { sameOrigin }
            }

            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.IF_REQUIRED
            }

            authorizeHttpRequests {
                authorize("/admin/login", permitAll)
                authorize("/admin/ott", permitAll)
                authorize("/admin/ott/request", permitAll)
                authorize("/admin/ott/verify", permitAll)
                authorize("/error", permitAll)
                authorize(anyRequest, hasRole(UserRole.ADMIN.name))
            }

            exceptionHandling {
                authenticationEntryPoint = LoginUrlAuthenticationEntryPoint("/admin/login")
            }

            oneTimeTokenLogin {
                showDefaultSubmitPage = false
                tokenGeneratingUrl = "/admin/ott/request"
                loginProcessingUrl = "/admin/ott/verify"
                oneTimeTokenGenerationSuccessHandler = imHereOttSuccessHandler
                authenticationSuccessHandler = ottLoginSuccessHandler
                authenticationFailureHandler = { _, response, _ ->
                    response.sendRedirect("/admin/ott?error=true")
                }
            }

            logout {
                logoutUrl = "/admin/logout"
                logoutSuccessUrl = "/admin/login?logout=true"
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(ottIpValidationFilter)
        }

        return http.build()
    }

    @Bean
    @Order(3)
    fun apiFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }
            cors { }

            headers {
                frameOptions { sameOrigin }
            }

            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            authorizeHttpRequests {
                securityWhiteList.whitelist.forEach { authorize(it, permitAll) }
                authorize("/error", permitAll)
                authorize(HttpMethod.OPTIONS, "/**", permitAll)
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

            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter())
        }

        return http.build()
    }
}
