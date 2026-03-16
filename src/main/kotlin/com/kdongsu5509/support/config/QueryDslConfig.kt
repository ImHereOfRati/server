package com.kdongsu5509.support.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QueryDslConfig(private val entityManager: EntityManager) {
    @Bean
    fun japQueryFactory(): JPAQueryFactory {
        return JPAQueryFactory(entityManager)
    }
}