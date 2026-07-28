package com.kdongsu5509.friends.scheduler

import com.kdongsu5509.friends.repository.FriendRelationRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class FriendRestrictionScheduler(
    private val friendRelationRepository: FriendRelationRepository
) {
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun cleanExpiredRestrictions() {
        friendRelationRepository.deleteExpired(LocalDateTime.now())
    }
}
