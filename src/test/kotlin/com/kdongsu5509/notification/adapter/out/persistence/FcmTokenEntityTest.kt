package com.kdongsu5509.imhere.notification.adapter.out.persistence

import com.kdongsu5509.imhere.auth.adapter.out.persistence.UserJpaEntity
import com.kdongsu5509.imhere.auth.domain.OAuth2Provider
import com.kdongsu5509.imhere.auth.domain.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test

class FcmTokenEntityTest {

    @Test
    @DisplayName("updateToken 메서드로 변경이 잘 일어난다")
    fun update_good() {
        //given
        val testToken = "testToken"
        val testEmail = "dongsu@test.com"
        val testFcmTokenEntity = createTestFcmTokenEntity(testToken, testEmail)

        //when
        testFcmTokenEntity.updateToken("newToken")

        //then
        assertThat(testFcmTokenEntity.token).isEqualTo("newToken")
        assertThat(testFcmTokenEntity.user.email).isEqualTo(testEmail)

    }

    private fun createTestFcmTokenEntity(testToken: String, testEmail: String): FcmTokenEntity {
        val testUserJpaEntity = UserJpaEntity(
            testEmail,
            UserRole.NORMAL,
            OAuth2Provider.KAKAO
        )
        return FcmTokenEntity(
            token = testToken,
            user = testUserJpaEntity
        )
    }

}