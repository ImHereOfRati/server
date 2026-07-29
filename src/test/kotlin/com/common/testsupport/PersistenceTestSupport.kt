package com.common.testsupport

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.kdongsu5509.support.config.LocalCacheConfig
import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.user.repository.jpa.SpringQueryDSLUserRepository
import com.solapi.sdk.message.service.DefaultMessageService
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class,
    LocalCacheConfig::class
)
abstract class PersistenceTestSupport {

    @MockitoBean
    lateinit var firebaseMessaging: FirebaseMessaging

    @MockitoBean
    lateinit var firebaseApp: FirebaseApp

    @MockitoBean
    lateinit var defaultMessageService: DefaultMessageService

}
