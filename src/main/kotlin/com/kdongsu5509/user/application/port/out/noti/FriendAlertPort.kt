package com.kdongsu5509.user.application.port.out.noti

import com.kdongsu5509.user.application.dto.AlertInformation

interface FriendAlertPort {
    fun sendAlert(alertInformation: AlertInformation)
}
