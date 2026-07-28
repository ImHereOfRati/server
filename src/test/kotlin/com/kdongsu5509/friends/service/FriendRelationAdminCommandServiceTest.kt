package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.repository.FriendRelationRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.then
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRelationAdminCommandServiceTest {

    @Mock
    lateinit var friendRelationRepository: FriendRelationRepository

    @InjectMocks
    lateinit var friendRelationAdminCommandService: FriendRelationAdminCommandService

    @Test
    @DisplayName("삭제는 상태와 무관하게 ID로 관계를 지운다")
    fun deleteById() {
        val id = UUID.randomUUID()

        friendRelationAdminCommandService.deleteById(id)

        then(friendRelationRepository).should().deleteById(id)
    }
}
