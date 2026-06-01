package com.kdongsu5509.friends.scheduler

import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FriendRestrictionSchedulerTest {

    @Mock
    private lateinit var friendRestrictionRepository: FriendRestrictionRepository

    private lateinit var scheduler: FriendRestrictionScheduler

    @BeforeEach
    fun setUp() {
        scheduler = FriendRestrictionScheduler(friendRestrictionRepository)
    }

    @Test
    @DisplayName("스케줄러 동작 시 만료된 차단/제한 내역을 삭제한다")
    fun cleanExpiredRestrictions() {
        scheduler.cleanExpiredRestrictions()

        verify(friendRestrictionRepository).deleteExpiredRestrictions()
    }
}
