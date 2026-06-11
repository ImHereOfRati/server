package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class SpringDataFriendRestrictionRepositoryTest @Autowired constructor(
    private val em: EntityManager,
    private val repository: SpringDataFriendRestrictionRepository
) {
    @Test
    @DisplayName("행위자 이메일과 페이징 정보로 차단 목록 슬라이스를 조회한다")
    fun findByRestrictorEmail_slice_success() {
        // given
        val actor = createUser("actor@test.com", "actor")
        val target1 = createUser("target1@test.com", "target1")
        val target2 = createUser("target2@test.com", "target2")

        em.persist(FriendRestrictionJpaEntity.create(actor, target1, FriendRestrictionType.BLOCK))
        em.persist(FriendRestrictionJpaEntity.create(actor, target2, FriendRestrictionType.BLOCK))
        em.flush()

        val pageable = PageRequest.of(0, 1)

        // when
        val result = repository.findByRestrictorEmail(actor.email, pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.hasNext()).isTrue()
    }

    @Test
    @DisplayName("행위자 이메일과 대상 ID, 타입으로 제한 목록을 삭제한다")
    fun deleteByRestrictorEmailAndRestrictedIdAndType_success() {
        // given
        val actor = createUser("actor@test.com", "actor")
        val target = createUser("target@test.com", "target")

        em.persist(FriendRestrictionJpaEntity.create(actor, target, FriendRestrictionType.BLOCK))
        em.flush()

        // when
        repository.deleteByRestrictorEmailAndRestrictedIdAndType(actor.email, target.id!!, FriendRestrictionType.BLOCK)
        em.flush()
        em.clear()

        // then
        val exists = repository.existsByRestrictorEmailAndRestrictedEmail(actor.email, target.email)
        assertThat(exists).isFalse()
    }

    @Test
    @DisplayName("만료 시간이 지난 제한 목록을 일괄 삭제한다")
    fun deleteExpired_deletesCorrectly() {
        // given
        val actor = createUser("actor@test.com", "actor")
        val target1 = createUser("target1@test.com", "target1")
        val target2 = createUser("target2@test.com", "target2")

        val now = LocalDateTime.of(2026, 5, 21, 12, 0)

        // 만료 안 됨
        val active = FriendRestrictionJpaEntity.create(actor, target1, FriendRestrictionType.REJECT)
        ReflectionTestUtils.setField(active, "expiredAt", now.plusDays(1))
        em.persist(active)

        // 만료됨
        val expired = FriendRestrictionJpaEntity.create(actor, target2, FriendRestrictionType.REJECT)
        ReflectionTestUtils.setField(expired, "expiredAt", now.minusDays(1))
        em.persist(expired)

        em.flush()

        // when
        repository.deleteExpired(now)
        em.flush()
        em.clear()

        // then
        val activeResult = repository.findByRestrictorEmail(actor.email, PageRequest.of(0, 10))
        assertThat(activeResult.content).hasSize(1)
        assertThat(activeResult.content[0].restricted.email).isEqualTo(target1.email)

        // 전체 테이블 확인
        val count = em.createQuery("select count(fr) from FriendRestrictionJpaEntity fr", java.lang.Long::class.java)
            .singleResult
        assertThat(count).isEqualTo(1L)
    }

    @Test
    @DisplayName("이메일 기반으로 차단된 유저가 존재하는지 확인한다")
    fun existsByRestrictorEmailAndRestrictedEmail_success() {
        // given
        val actor = createUser("actor@test.com", "actor")
        val target = createUser("target@test.com", "target")

        em.persist(FriendRestrictionJpaEntity.create(actor, target, FriendRestrictionType.BLOCK))
        em.flush()

        // when
        val exists = repository.existsByRestrictorEmailAndRestrictedEmail(actor.email, target.email)
        val notExists = repository.existsByRestrictorEmailAndRestrictedEmail(actor.email, "other@test.com")

        // then
        assertThat(exists).isTrue()
        assertThat(notExists).isFalse()
    }

    private fun createUser(email: String, nickname: String): UserJpaEntity {
        val user = UserJpaEntity(
            email = email,
            nickname = nickname,
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        em.persist(user)
        return user
    }
}
