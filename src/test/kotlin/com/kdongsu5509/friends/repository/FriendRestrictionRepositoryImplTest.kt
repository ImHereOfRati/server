package com.kdongsu5509.friends.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.repository.jpa.FriendRestrictionJpaEntity
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendRestrictionRepository
import com.kdongsu5509.friends.repository.mapper.FriendRestrictionMapper
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRestrictionRepositoryImplTest {

    @Mock
    lateinit var entityManager: EntityManager

    @Mock
    lateinit var friendRestrictionMapper: FriendRestrictionMapper

    @Mock
    lateinit var springDataFriendRestrictionRepository: SpringDataFriendRestrictionRepository

    @InjectMocks
    lateinit var friendRestrictionRepositoryImpl: FriendRestrictionRepositoryImpl

    private fun createTestUser(id: UUID): User = User(
        id = id,
        email = "test@test.com",
        nickname = "test",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private fun createTestUserEntity(id: UUID): UserJpaEntity = UserJpaEntity(
        email = "test@test.com",
        nickname = "test",
        role = UserRole.NORMAL,
        provider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    ).apply { this.id = id }

    // 1. save
    @Nested
    @DisplayName("save 메서드는")
    inner class SaveTest {
        @Test
        @DisplayName("친구 제한(차단/거절)을 성공적으로 저장한다")
        fun success() {
            // given
            val restrictorId = UUID.randomUUID()
            val restrictedId = UUID.randomUUID()

            val restrictor = createTestUser(restrictorId)
            val restricted = createTestUser(restrictedId)

            val restrictorEntity = createTestUserEntity(restrictorId)
            val restrictedEntity = createTestUserEntity(restrictedId)

            val domain = FriendRestriction(
                id = UUID.randomUUID(),
                restrictor = restrictor,
                restricted = restricted,
                type = FriendRestrictionType.BLOCK,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                expiredAt = null
            )

            val entity =
                FriendRestrictionJpaEntity.create(restrictorEntity, restrictedEntity, FriendRestrictionType.BLOCK)

            `when`(entityManager.getReference(UserJpaEntity::class.java, restrictorId)).thenReturn(restrictorEntity)
            `when`(entityManager.getReference(UserJpaEntity::class.java, restrictedId)).thenReturn(restrictedEntity)
            `when`(springDataFriendRestrictionRepository.save(any())).thenReturn(entity)
            `when`(friendRestrictionMapper.toDomain(entity)).thenReturn(domain)

            // when
            val result = friendRestrictionRepositoryImpl.save(domain)

            // then
            assertThat(result).isEqualTo(domain)
            verify(springDataFriendRestrictionRepository).save(any())
        }
    }

    // 2. findById
    @Nested
    @DisplayName("findById 메서드는")
    inner class FindByIdTest {
        @Test
        @DisplayName("ID로 친구 제한을 단건 조회한다")
        fun success() {
            val id = UUID.randomUUID()
            val entity = FriendRestrictionJpaEntity.create(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                FriendRestrictionType.BLOCK
            ).apply { ReflectionTestUtils.setField(this, "id", id) }

            val domain = FriendRestriction(
                id = id,
                restrictor = createTestUser(UUID.randomUUID()),
                restricted = createTestUser(UUID.randomUUID()),
                type = FriendRestrictionType.BLOCK,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                expiredAt = null
            )

            `when`(springDataFriendRestrictionRepository.findById(id)).thenReturn(Optional.of(entity))
            `when`(friendRestrictionMapper.toDomain(entity)).thenReturn(domain)

            val result = friendRestrictionRepositoryImpl.findById(id)

            assertThat(result).isEqualTo(domain)
        }

        @Test
        @DisplayName("존재하지 않는 ID면 null을 반환한다")
        fun null_when_not_found() {
            val id = UUID.randomUUID()
            `when`(springDataFriendRestrictionRepository.findById(id)).thenReturn(Optional.empty())

            val result = friendRestrictionRepositoryImpl.findById(id)

            assertThat(result).isNull()
        }
    }

    // 3. findAllByEmail
    @Nested
    @DisplayName("findAllByEmail 메서드는")
    inner class FindAllByEmailTest {
        @Test
        @DisplayName("행위자 이메일로 차단 목록 슬라이스를 조회한다")
        fun success() {
            // given
            val email = "actor@test.com"
            val pageable = PageRequest.of(0, 10)

            val entity = FriendRestrictionJpaEntity.create(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                FriendRestrictionType.BLOCK
            )
            val slice = PageImpl(listOf(entity), pageable, 1L)
            val domain = FriendRestriction(
                id = UUID.randomUUID(),
                restrictor = createTestUser(UUID.randomUUID()),
                restricted = createTestUser(UUID.randomUUID()),
                type = FriendRestrictionType.BLOCK,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                expiredAt = null
            )

            `when`(springDataFriendRestrictionRepository.findByRestrictorEmail(email, pageable)).thenReturn(slice)
            `when`(friendRestrictionMapper.toDomain(entity)).thenReturn(domain)

            // when
            val result = friendRestrictionRepositoryImpl.findAllByEmail(email, pageable)

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(domain)
        }
    }

    // 4. findAll
    @Nested
    @DisplayName("findAll 메서드는")
    inner class FindAllTest {
        @Test
        @DisplayName("전체 친구 제한 슬라이스를 조회한다")
        fun success() {
            val pageable = PageRequest.of(0, 10)
            val entity = FriendRestrictionJpaEntity.create(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                FriendRestrictionType.BLOCK
            )
            val slice = PageImpl(listOf(entity), pageable, 1L)
            val domain = FriendRestriction(
                id = UUID.randomUUID(),
                restrictor = createTestUser(UUID.randomUUID()),
                restricted = createTestUser(UUID.randomUUID()),
                type = FriendRestrictionType.BLOCK,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                expiredAt = null
            )

            `when`(springDataFriendRestrictionRepository.findAll(pageable)).thenReturn(slice)
            `when`(friendRestrictionMapper.toDomain(entity)).thenReturn(domain)

            val result = friendRestrictionRepositoryImpl.findAll(pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(domain)
        }
    }

    // 5. deleteById
    @Nested
    @DisplayName("deleteById 메서드는")
    inner class DeleteByIdTest {
        @Test
        @DisplayName("ID로 친구 제한을 삭제한다")
        fun success() {
            val id = UUID.randomUUID()

            friendRestrictionRepositoryImpl.deleteById(id)

            verify(springDataFriendRestrictionRepository).deleteById(id)
        }
    }

    // 6. deleteBlockByRestrictorEmailAndRestrictedId
    @Nested
    @DisplayName("deleteBlockByRestrictorEmailAndRestrictedId 메서드는")
    inner class DeleteBlockByRestrictorEmailAndRestrictedIdTest {
        @Test
        @DisplayName("차단자의 이메일과 차단된 자의 ID로 차단 관계를 삭제한다")
        fun success() {
            val email = "test@test.com"
            val targetId = UUID.randomUUID()

            friendRestrictionRepositoryImpl.deleteBlockByRestrictorEmailAndRestrictedId(email, targetId)

            verify(springDataFriendRestrictionRepository)
                .deleteByRestrictorEmailAndRestrictedIdAndType(email, targetId, FriendRestrictionType.BLOCK)
        }
    }

    // 7. deleteExpiredRestrictions
    @Nested
    @DisplayName("deleteExpiredRestrictions 메서드는")
    inner class DeleteExpiredRestrictionsTest {
        @Test
        @DisplayName("만료된 제한 관계를 모두 삭제한다")
        fun success() {
            friendRestrictionRepositoryImpl.deleteExpiredRestrictions()

            verify(springDataFriendRestrictionRepository).deleteExpired(any())
        }
    }

    // 8. existsRestriction
    @Nested
    @DisplayName("existsRestriction 메서드는")
    inner class ExistsRestrictionTest {
        @Test
        @DisplayName("차단 관계가 존재하는지 확인한다")
        fun success() {
            val requesterEmail = "req@test.com"
            val targetEmail = "tar@test.com"

            `when`(
                springDataFriendRestrictionRepository.existsByRestrictorEmailAndRestrictedEmail(
                    requesterEmail,
                    targetEmail
                )
            ).thenReturn(true)

            val result = friendRestrictionRepositoryImpl.existsRestriction(requesterEmail, targetEmail)

            assertThat(result).isTrue()
        }
    }
}
