package com.kdongsu5509.support.config

import com.kdongsu5509.support.external.DiscordOttSuccessHandler
import com.kdongsu5509.support.external.OttLoginSuccessHandler
import com.kdongsu5509.user.application.service.user.AdminSecretFilter
import com.kdongsu5509.user.application.service.user.JwtAuthenticationFilter
import com.kdongsu5509.user.domain.user.UserRole
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.ott.GenerateOneTimeTokenFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true
)
@EnableConfigurationProperties(SecurityConstants::class)
class SecurityConfig(
    private val securityConstants: SecurityConstants,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val discordOttSuccessHandler: DiscordOttSuccessHandler,
    private val ottLoginSuccessHandler: OttLoginSuccessHandler,
    @param:Value("\${admin.secret}") private val adminSecret: String,
    @param:Value("\${admin.id}") private val adminId: String
) {

    @Bean
    fun userDetailsService(): UserDetailsService {
        val adminUserDetails = User.withUsername(adminId)
            .password("{noop}N/A")
            .roles(UserRole.ADMIN.name)
            .build()

        return UserDetailsService { email ->
            if (email == adminId) {
                adminUserDetails
            } else {
                throw UsernameNotFoundException("User not found with email: $email")
            }
        }
    }

    @Bean
    fun oneTimeTokenService(jdbcOperation: JdbcOperations): OneTimeTokenService = JdbcOneTimeTokenService(
        jdbcOperation
    )

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
                securityConstants.whitelist.forEach { authorize(it, permitAll) }
                authorize("/api/admin/auth/**", permitAll) // OTT 로그인 엔드포인트
                authorize("/api/admin/**", hasRole(UserRole.ADMIN.name))
                authorize(anyRequest, authenticated)
            }

            oneTimeTokenLogin {
                showDefaultSubmitPage = false
                tokenGeneratingUrl = "/api/admin/auth/ott"
                loginProcessingUrl = "/api/admin/auth"
                oneTimeTokenGenerationSuccessHandler = discordOttSuccessHandler // 1. 발급 성공 시 메신저 발송
                authenticationSuccessHandler = ottLoginSuccessHandler // 2. 인증(소모) 성공 시 처리 (JWT 발급 등)
                authenticationFailureHandler = { _, response, _ ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                }
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
            addFilterBefore<GenerateOneTimeTokenFilter>(AdminSecretFilter(adminSecret))
        }

        return http.build()
    }
}
