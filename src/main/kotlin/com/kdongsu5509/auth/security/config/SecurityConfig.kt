package com.kdongsu5509.auth.security.config

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.security.ActiveUserAuthorizationManager
import com.kdongsu5509.auth.security.RestControllerMethodPointcut
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.auth.security.filter.JwtAuthenticationFilter
import com.kdongsu5509.shared.response.APIResponseSerializers
import com.kdongsu5509.user.domain.UserRole
import org.springframework.aop.Advisor
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Role
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@EnableConfigurationProperties(SecurityWhiteList::class)
class SecurityConfig(
    private val securityWhiteList: SecurityWhiteList,
    private val imHereJwtTokenParserPort: ImHereTokenParserPort,
    @param:Value("\${admin.id}") private val adminId: String,
    @param:Value("\${management.endpoints.web.base-path:/actuator}") private val managementBasePath: String,
) {
    private val permitAllPaths by lazy { securityWhiteList.permitAllPaths(managementBasePath) }

    @Bean fun jwtAuthenticationFilter(): JwtAuthenticationFilter = JwtAuthenticationFilter(imHereJwtTokenParserPort, permitAllPaths)

    @Bean @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    fun activeUserMethodInterceptor(): Advisor = AuthorizationManagerBeforeMethodInterceptor(
        RestControllerMethodPointcut(), ActiveUserAuthorizationManager(permitAllPaths)
    )

    @Bean fun userDetailsService(): UserDetailsService = UserDetailsService { email ->
        if (email == adminId) User.withUsername(adminId).password("{noop}N/A").roles(UserRole.ADMIN.name).build()
        else throw UsernameNotFoundException("User not found: $email")
    }

    @Bean fun corsConfigurationSource(): CorsConfigurationSource = CorsConfiguration().apply {
        allowedOrigins = securityWhiteList.corsAllowedOrigins
        allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        allowedHeaders = listOf("Authorization", "Content-Type", "X-Requested-With")
        allowCredentials = false
    }.let { configuration -> UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", configuration) } }

    @Bean @Order(1)
    fun adminApiFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.securityMatcher("/api/admin/**")
        http {
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }
            cors { }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                permitAllPaths.forEach { authorize(it, permitAll) }
                authorize(anyRequest, hasRole(UserRole.ADMIN.name))
            }
            exceptionHandling {
                authenticationEntryPoint = { _, response, _ -> response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized") }
                accessDeniedHandler = { _, response, _ -> APIResponseSerializers.writeErrorResponse(response, HttpStatus.FORBIDDEN, AuthException.IMHERE_ACCESS_DENIED.imhereErrorCode, AuthException.IMHERE_ACCESS_DENIED.errorMessage) }
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter())
        }
        return http.build()
    }

    @Bean @Order(2)
    fun apiFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }
            cors { }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                permitAllPaths.forEach { authorize(it, permitAll) }
                authorize("/error", permitAll)
                authorize(HttpMethod.OPTIONS, "/**", permitAll)
                authorize(anyRequest, authenticated)
            }
            exceptionHandling {
                authenticationEntryPoint = { _, response, _ -> APIResponseSerializers.writeErrorResponse(response, HttpStatus.UNAUTHORIZED, AuthException.IMHERE_INVALID_TOKEN.imhereErrorCode, AuthException.IMHERE_INVALID_TOKEN.errorMessage) }
                accessDeniedHandler = { _, response, _ -> APIResponseSerializers.writeErrorResponse(response, HttpStatus.FORBIDDEN, AuthException.IMHERE_ACCESS_DENIED.imhereErrorCode, AuthException.IMHERE_ACCESS_DENIED.errorMessage) }
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter())
        }
        return http.build()
    }
}
