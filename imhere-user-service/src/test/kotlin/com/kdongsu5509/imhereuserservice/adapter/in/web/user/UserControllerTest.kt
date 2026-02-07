package com.kdongsu5509.imhereuserservice.adapter.`in`.web.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var springDataUserRepository: SpringDataUserRepository

    @Autowired
    lateinit var mockMvc: MockMvc

    companion object {
        const val userSearchUrlWithoutKeyword = "/api/v1/user/info/"
        const val userSearchMeUrl = "/api/v1/user/info/me"
        const val DSKO = "고동수"
        const val TEST_EMAIL = "test@test.com"
    }

    @Test
    @DisplayName("없는 사람 조회")
    fun searchUsers() {

        val testKeyword = "none"
        mockMvc.perform(
            get(userSearchUrlWithoutKeyword + testKeyword)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("있는 사람 조회")
    fun searchUsers_with_really_exist() {
        //given
        val testUsersButSizeIsOne = createTestUser(1)
        springDataUserRepository.save(
            testUsersButSizeIsOne[0]
        )
        val testKeyword = "고동수"

        //when, then
        mockMvc.perform(
            get(userSearchUrlWithoutKeyword + testKeyword)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].userEmail").value(testUsersButSizeIsOne[0].email))
            .andExpect(jsonPath("$.data[0].userNickname").value(DSKO))
    }

    @Test
    @DisplayName("닉네임 맞는 사람 모두 조회")
    fun searchUsers_with_really_exist_all() {
        //given
        springDataUserRepository.saveAll(createTestUser(10))

        //when, then
        mockMvc.perform(
            get(userSearchUrlWithoutKeyword + DSKO)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data", hasSize<Any>(10)))
            .andExpect(jsonPath("$.data[0].userNickname").value(DSKO))
    }

    private fun createTestUser(size: Int): List<UserJpaEntity> {
        val list = mutableListOf<UserJpaEntity>()

        for (i in 0 until size) {
            val testEmail = "test$i@test.com"

            val testUserEntity = UserJpaEntity(
                email = testEmail,
                nickname = DSKO,
                role = UserRole.NORMAL,
                provider = OAuth2Provider.KAKAO,
                status = UserStatus.ACTIVE
            )
            list.add(testUserEntity)
        }

        return list
    }

    @Test
    @WithMockUser(username = TEST_EMAIL)
    @DisplayName("내 정보 조회")
    fun searchMe_success() {
        //given
        val testUser = UserJpaEntity(
            email = TEST_EMAIL,
            nickname = DSKO,
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        springDataUserRepository.save(testUser)

        //when, then
        mockMvc.perform(
            get(userSearchMeUrl)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userEmail").value(TEST_EMAIL))
            .andExpect(jsonPath("$.data.userNickname").value(DSKO))
    }
}