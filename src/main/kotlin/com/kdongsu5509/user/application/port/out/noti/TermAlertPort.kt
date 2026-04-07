package com.kdongsu5509.user.application.port.out.noti

import com.kdongsu5509.user.application.dto.AlertInformation

interface TermAlertPort {
    fun sendAlert(alertInformation: AlertInformation)
}
