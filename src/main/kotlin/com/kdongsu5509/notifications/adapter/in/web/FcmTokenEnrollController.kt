package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.notifications.adapter.`in`.web.dto.FcmTokenInfo
import com.kdongsu5509.notifications.application.port.`in`.ManageFcmTokenUseCasePort
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notification", version = "1")
class FcmTokenEnrollController(
    private val enrollFcmTokenUserCasePort: ManageFcmTokenUseCasePort
) {
    @PostMapping("/fcmToken")
    fun enroll(
        @AuthenticationPrincipal userDetails: UserDetails,
        @Validated @RequestBody fcmTokenInfo: FcmTokenInfo
    ) {
        val userEmail = userDetails.username
        enrollFcmTokenUserCasePort.save(fcmTokenInfo.fcmToken, userEmail, fcmTokenInfo.deviceType)
    }
}
