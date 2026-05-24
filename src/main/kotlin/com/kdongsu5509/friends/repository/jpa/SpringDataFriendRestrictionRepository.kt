package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.friends.domain.FriendRestrictionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.repository.query.Param
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

@Repository
interface SpringDataFriendRestrictionRepository : JpaRepository<FriendRestrictionJpaEntity, UUID> {
    fun findByRestrictorEmail(email: String, pageable: Pageable): Slice<FriendRestrictionJpaEntity>
    fun deleteByRestrictorEmailAndRestrictedIdAndType(
        restrictorEmail: String,
        restrictedId: UUID,
        type: FriendRestrictionType
    )

    @Modifying(clearAutomatically = true)
    @Query(
        """
        delete from FriendRestrictionJpaEntity fr
        where fr.expiredAt <= :now
    """
    )
    fun deleteExpired(@Param("now") now: LocalDateTime)

    fun existsByRestrictorEmailAndRestrictedEmail(requesterEmail: String, targetEmail: String): Boolean
}
