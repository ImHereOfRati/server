package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FcmTokenPersistenceAdapterTest {

    @Mock
    private lateinit var fcmTokenMapper: FcmTokenMapper

    @Mock
    private lateinit var repository: SpringDataFcmTokenRepository

    private lateinit var adapter: FcmTokenPersistenceAdapter

    @BeforeEach
    fun setUp() {
        adapter = FcmTokenPersistenceAdapter(fcmTokenMapper, repository)
    }

    @Test
    @DisplayName("FcmToken을 저장하면 Entity로 변환되어 repository를 통해 저장된다")
    fun save_success() {
        // given
        val domain = FcmToken(email = "test@ex.com", fcmToken = "token", deviceType = DeviceType.AOS)
        val entity = FcmTokenJpaEntity(email = "test@ex.com", token = "token", deviceType = DeviceType.AOS)
        
        `when`(fcmTokenMapper.toEntity(domain)).thenReturn(entity)

        // when
        adapter.save(domain)

        // then
        verify(fcmTokenMapper).toEntity(domain)
        verify(repository).save(entity)
    }

    @Test
    @DisplayName("이메일로 조회 시 Entity가 존재하면 Domain으로 변환하여 반환한다")
    fun findByUserEmail_success() {
        // given
        val email = "test@ex.com"
        val entity = FcmTokenJpaEntity(email = email, token = "token", deviceType = DeviceType.AOS)
        val domain = FcmToken(email = email, fcmToken = "token", deviceType = DeviceType.AOS)

        `when`(repository.findByEmail(email)).thenReturn(entity)
        `when`(fcmTokenMapper.toDomain(entity)).thenReturn(domain)

        // when
        val result = adapter.findByUserEmail(email)

        // then
        assertThat(result).isEqualTo(domain)
    }

    @Test
    @DisplayName("이메일로 조회 시 Entity가 없으면 null을 반환한다")
    fun findByUserEmail_returnsNull() {
        // given
        val email = "test@ex.com"
        `when`(repository.findByEmail(email)).thenReturn(null)

        // when
        val result = adapter.findByUserEmail(email)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("ID로 토큰을 정상 삭제한다")
    fun deleteById_success() {
        // given
        val tokenId = 100L

        // when
        adapter.deleteById(tokenId)

        // then
        verify(repository).deleteById(tokenId)
    }
}
