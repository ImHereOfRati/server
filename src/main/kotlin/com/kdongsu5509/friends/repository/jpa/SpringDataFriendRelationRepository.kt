package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.friends.domain.FriendRelationStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface SpringDataFriendRelationRepository : JpaRepository<FriendRelationJpaEntity, UUID> {

    fun findByLowUserIdAndHighUserId(lowUserId: UUID, highUserId: UUID): FriendRelationJpaEntity?

    fun findByIdAndStatus(id: UUID, status: FriendRelationStatus): FriendRelationJpaEntity?

    fun findByLowUserIdAndHighUserIdAndStatus(
        lowUserId: UUID,
        highUserId: UUID,
        status: FriendRelationStatus
    ): FriendRelationJpaEntity?

    fun findAllByStatus(status: FriendRelationStatus, pageable: Pageable): Slice<FriendRelationJpaEntity>

    fun findAllByStatusIn(
        statuses: Collection<FriendRelationStatus>,
        pageable: Pageable
    ): Slice<FriendRelationJpaEntity>

    @Modifying(clearAutomatically = true)
    @Query(
        "delete from FriendRelationJpaEntity r " +
                "where r.expiredAt is not null " +
                "and " +
                "r.expiredAt <= :now"
    )
    fun deleteExpired(@Param("now") now: LocalDateTime)
}
