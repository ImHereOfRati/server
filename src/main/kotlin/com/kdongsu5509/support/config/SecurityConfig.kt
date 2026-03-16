package com.kdongsu5509.support.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

//TODO : change this. refer to comment of this file
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true
)
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            formLogin { disable() }
            httpBasic { disable() }

            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }
}

//package com.kdongsu5509.imhereapigateway.filter;
//
//import com.kdongsu5509.imhereapigateway.filter.jwt.JwtTokenUtil;
//import java.util.List;
//import lombok.Getter;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.util.AntPathMatcher;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//@Slf4j
//@Component
//public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
//
//    private final JwtTokenUtil jwtTokenUtil;
//    private final AntPathMatcher pathMatcher = new AntPathMatcher(); // 패턴 매칭용
//
//    public AuthorizationHeaderFilter(JwtTokenUtil jwtTokenUtil) {
//        super(Config.class);
//        this.jwtTokenUtil = jwtTokenUtil;
//    }
//
//    @Getter
//    @Setter
//    public static class Config {
//        private List<String> excludePaths; // YAML에서 받을 제외 목록
//    }
//
//    @Override
//    public GatewayFilter apply(Config config) {
//        return (exchange, chain) -> {
//            ServerHttpRequest request = exchange.getRequest();
//            String path = request.getURI().getPath();
//
//            if (isWhiteListEndPoint(config, path)) {
//                return chain.filter(exchange);
//            }
//
//            // 2. 인증 헤더 존재 여부 확인
//            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
//                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
//            }
//
//            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//            String jwt = authorizationHeader.replace("Bearer ", "");
//
//            // 3. 토큰 유효성 검사
//            if (!jwtTokenUtil.validateToken(jwt)) {
//                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
//            }
//
//            // 4. 헤더 전파 및 필터 체인 진행
//            return chain.filter(exchange.mutate()
//                    .request(propagateHeader(exchange, jwt))
//                    .build()
//            );
//        };
//    }
//
//    private boolean isWhiteListEndPoint(Config config, String path) {
//        if (config.getExcludePaths() != null) {
//            for (String excludePath : config.getExcludePaths()) {
//                if (pathMatcher.match(excludePath, path)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    private ServerHttpRequest propagateHeader(ServerWebExchange exchange, String jwt) {
//        String subject = jwtTokenUtil.getUsernameFromToken(jwt);
//        String role = jwtTokenUtil.getRoleFromToken(jwt);
//
//        return exchange.getRequest().mutate()
//                .header("X-User-Email", subject)
//                .header("X-User-Role", role)
//                .build();
//    }
//
//    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
//        ServerHttpResponse response = exchange.getResponse();
//        response.setStatusCode(httpStatus);
//        log.error("Filter Error: {}, Status: {}", err, httpStatus);
//        return response.setComplete();
//    }
//}