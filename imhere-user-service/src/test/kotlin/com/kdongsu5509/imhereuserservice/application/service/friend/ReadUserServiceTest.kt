package com.kdongsu5509.imhereuserservice.application.service.friend

import com.kdongsu5509.imhereuserservice.application.dto.UserInformation
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserLoadPort
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.User
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ReadUserServiceTest {
    @Mock
    lateinit var userLoadPort: UserLoadPort

    @InjectMocks
    lateinit var userSearchService: ReadUserService

    companion object {
        const val TEST_KEYWORD = "테스트"
        const val TEST_EMAIL = "test@test.com"
        const val TEST_NICKNAME = "테스트"
        val testOauthProvider = OAuth2Provider.KAKAO
        val testUserStatus = UserStatus.ACTIVE
        val testRole = UserRole.NORMAL
        val testUser = User(TEST_EMAIL, TEST_NICKNAME, testOauthProvider, testRole, testUserStatus)
    }

    @Test
    @DisplayName("찾은 사용자가 1명 이상이면 User 리스트로 잘 반환해서 나간다")
    fun searchUser_overThanOne() {
        //given
        `when`(userLoadPort.findByEmailAndNickname(TEST_KEYWORD)).thenReturn(
            listOf(testUser)
        )

        //when, then
        assertDoesNotThrow {
            userSearchService.searchUser(TEST_KEYWORD)
        }
    }

    @Test
    @DisplayName("찾은 사용자가 0명 이어도 User 리스트로 잘 반환해서 나간다")
    fun searchUser_zero() {
        // given
        val testKeyword = "존재하지않음"
        `when`(userLoadPort.findByEmailAndNickname(testKeyword)).thenReturn(
            listOf()
        )

        // when
        val result = userSearchService.searchUser(testKeyword)

        // then
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isEmpty()
        Assertions.assertThat(result).isInstanceOf(List::class.java)
    }

    @Test
    @DisplayName("사용자의 정보를 잘 찾아오면 변환해서 잘 나간다.")
    fun searchMe() {
        //given
        `when`(userLoadPort.findActiveUserByEmailOrNull(TEST_KEYWORD)).thenReturn(
            testUser
        )

        //when, then
        var result: UserInformation? = null
        assertDoesNotThrow {
            result = userSearchService.searchMe(TEST_KEYWORD)
        }
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isInstanceOf(UserInformation::class.java)
    }
}