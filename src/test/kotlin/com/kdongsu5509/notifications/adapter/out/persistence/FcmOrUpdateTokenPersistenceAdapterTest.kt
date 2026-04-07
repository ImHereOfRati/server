package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class FcmOrUpdateTokenPersistenceAdapterTest {

    @Mock
    private lateinit var fcmTokenMapper: FcmTokenMapper

    @Mock
    private lateinit var springDataFcmTokenRepository: SpringDataFcmTokenRepository

    @InjectMocks
    private lateinit var adapter: FcmOrUpdateTokenPersistenceAdapter

    companion object {
        const val USER_EMAIL = "user@example.com"
        const val OLD_TOKEN = "old-token"
        const val NEW_TOKEN = "new-token"
    }

    @Test
    @DisplayName("기존 엔티티가 없으면 새로운 엔티티 저장")
    fun saveOrUpdate_new_entity() {
        // given
        `when`(springDataFcmTokenRepository.findByUserEmail(USER_EMAIL)).thenReturn(null)

        `when`(springDataFcmTokenRepository.save(any(FcmTokenJpaEntity::class.java)))
            .thenAnswer { it.arguments[0] as FcmTokenJpaEntity }

        // when
        adapter.saveOrUpdate(USER_EMAIL, NEW_TOKEN, DeviceType.AOS)


        // then
        verify(springDataFcmTokenRepository).save(any(FcmTokenJpaEntity::class.java))
    }

    @Test
    @DisplayName("기존 엔티티가 있고 토큰이 다르면 업데이트")
    fun saveOrUpdate_update_existing_entity() {
        // given
        val existingEntity = spy(FcmTokenJpaEntity(OLD_TOKEN, USER_EMAIL, DeviceType.AOS))
        `when`(springDataFcmTokenRepository.findByUserEmail(USER_EMAIL)).thenReturn(existingEntity)

        // when
        adapter.saveOrUpdate(USER_EMAIL, NEW_TOKEN, DeviceType.AOS)

        // then
        verify(existingEntity).updateToken(NEW_TOKEN)
        // Note: Spring Data JPA handles dirty checking, so we don't explicitly call save() if updated
    }

    @Test
    @DisplayName("기존 엔티티가 있고 토큰이 같으면 아무것도 하지 않음")
    fun saveOrUpdate_no_change() {
        // given
        val existingEntity = spy(FcmTokenJpaEntity(NEW_TOKEN, USER_EMAIL, DeviceType.AOS))
        `when`(springDataFcmTokenRepository.findByUserEmail(USER_EMAIL)).thenReturn(existingEntity)

        // when
        adapter.saveOrUpdate(USER_EMAIL, NEW_TOKEN, DeviceType.AOS)

        // then
        verify(existingEntity, never()).updateToken(anyString())
        verify(springDataFcmTokenRepository, never()).save(any())
    }

    @Test
    @DisplayName("이메일로 토큰 조회 시 도메인 엔티티로 변환하여 반환")
    fun findByUserEmail_success() {
        // given
        val jpaEntity = FcmTokenJpaEntity(NEW_TOKEN, USER_EMAIL, DeviceType.AOS)
        val domainEntity = FcmToken(1L, USER_EMAIL, NEW_TOKEN, DeviceType.AOS, LocalDateTime.now())

        `when`(springDataFcmTokenRepository.findByUserEmail(USER_EMAIL)).thenReturn(jpaEntity)
        `when`(fcmTokenMapper.mapToDomainEntity(jpaEntity)).thenReturn(domainEntity)

        // when
        val result = adapter.findByUserEmail(USER_EMAIL)

        // then
        assertThat(result).isEqualTo(domainEntity)
    }

    @Test
    @DisplayName("ID로 토큰 삭제")
    fun deleteById_success() {
        // when
        adapter.deleteById(1L)

        // then
        verify(springDataFcmTokenRepository).deleteById(1L)
    }
}
