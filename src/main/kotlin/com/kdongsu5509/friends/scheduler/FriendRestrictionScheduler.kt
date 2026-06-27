package com.kdongsu5509.friends.scheduler

import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FriendRestrictionScheduler(
    private val friendRestrictionRepository: FriendRestrictionRepository
) {
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun cleanExpiredRestrictions() {
        friendRestrictionRepository.deleteExpiredRestrictions()
    }
}
