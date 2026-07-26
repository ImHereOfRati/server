package com.kdongsu5509.friends.repository.mapper

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.repository.jpa.FriendRestrictionJpaEntity
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserMapper
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRestrictionMapperTest {

    @Mock
    lateinit var userMapper: UserMapper

    @InjectMocks
    lateinit var mapper: FriendRestrictionMapper

    @Test
    @DisplayName("FriendRestriction 엔티티를 도메인 객체로 성공적으로 변환한다")
    fun toDomain_success() {
        // given
        val restrictorId = UUID.randomUUID()
        val restrictedId = UUID.randomUUID()

        val restrictorEntity = UserJpaEntity(
            "restrictor@test.com", "restrictor", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )
        val restrictedEntity = UserJpaEntity(
            "restricted@test.com", "restricted", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )

        val restrictorDomain = User(
            id = restrictorId,
            email = "restrictor@test.com",
            nickname = "restrictor",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        val restrictedDomain = User(
            id = restrictedId,
            email = "restricted@test.com",
            nickname = "restricted",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )

        `when`(userMapper.toDomain(restrictorEntity)).thenReturn(restrictorDomain)
        `when`(userMapper.toDomain(restrictedEntity)).thenReturn(restrictedDomain)

        val entity = FriendRestrictionJpaEntity.create(
            restrictor = restrictorEntity,
            restricted = restrictedEntity,
            type = FriendRestrictionType.BLOCK
        )
        val id = UUID.randomUUID()
        ReflectionTestUtils.setField(entity, "id", id)

        val now = LocalDateTime.now()
        ReflectionTestUtils.setField(entity, "createdAt", now)
        ReflectionTestUtils.setField(entity, "updatedAt", now)

        // when
        val domain = mapper.toDomain(entity)

        // then
        assertThat(domain.id).isEqualTo(id)
        assertThat(domain.restrictor).isEqualTo(restrictorDomain)
        assertThat(domain.restricted).isEqualTo(restrictedDomain)
        assertThat(domain.type).isEqualTo(FriendRestrictionType.BLOCK)
        assertThat(domain.createdAt).isEqualTo(now)
        assertThat(domain.updatedAt).isEqualTo(now)
        // BLOCK 타입일 때는 expiredAt이 null이어야 함
        assertThat(domain.expiredAt).isNull()
    }

    @Test
    @DisplayName("FriendRestriction(BLOCK) 도메인 객체를 엔티티로 성공적으로 변환한다")
    fun toEntity_block_success() {
        // given
        val restrictorDomain = User(
            id = UUID.randomUUID(),
            email = "restrictor@test.com",
            nickname = "restrictor",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        val restrictedDomain = User(
            id = UUID.randomUUID(),
            email = "restricted@test.com",
            nickname = "restricted",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )

        val restrictorEntity = UserJpaEntity(
            "restrictor@test.com", "restrictor", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )
        val restrictedEntity = UserJpaEntity(
            "restricted@test.com", "restricted", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )

        `when`(userMapper.toEntity(restrictorDomain)).thenReturn(restrictorEntity)
        `when`(userMapper.toEntity(restrictedDomain)).thenReturn(restrictedEntity)

        val domain = FriendRestriction(
            id = UUID.randomUUID(),
            restrictor = restrictorDomain,
            restricted = restrictedDomain,
            type = FriendRestrictionType.BLOCK,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            expiredAt = LocalDateTime.now().plusDays(10)
        )

        // when
        val entity = mapper.toEntity(domain)

        // then
        assertThat(entity.restrictor).isEqualTo(restrictorEntity)
        assertThat(entity.restricted).isEqualTo(restrictedEntity)
        assertThat(entity.type).isEqualTo(FriendRestrictionType.BLOCK)
    }

    @Test
    @DisplayName("FriendRestriction(REJECT) 도메인 객체를 엔티티로 변환 시 도메인이 계산한 만료일을 그대로 전달한다")
    fun toEntity_reject_success() {
        // given
        val restrictorDomain = User(
            id = UUID.randomUUID(),
            email = "restrictor@test.com",
            nickname = "restrictor",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        val restrictedDomain = User(
            id = UUID.randomUUID(),
            email = "restricted@test.com",
            nickname = "restricted",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )

        val restrictorEntity = UserJpaEntity(
            "restrictor@test.com", "restrictor", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )
        val restrictedEntity = UserJpaEntity(
            "restricted@test.com", "restricted", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )

        `when`(userMapper.toEntity(restrictorDomain)).thenReturn(restrictorEntity)
        `when`(userMapper.toEntity(restrictedDomain)).thenReturn(restrictedEntity)

        // 도메인 팩토리가 만료일(30일 뒤)을 이미 계산해서 갖고 있는 상태
        val domain = FriendRestriction.reject(restrictorDomain, restrictedDomain)

        // when
        val entity = mapper.toEntity(domain)

        // then
        assertThat(entity.restrictor).isEqualTo(restrictorEntity)
        assertThat(entity.restricted).isEqualTo(restrictedEntity)
        assertThat(entity.type).isEqualTo(FriendRestrictionType.REJECT)
        assertThat(entity.expiredAt).isEqualTo(domain.expiredAt)
    }
}
