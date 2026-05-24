package com.kdongsu5509.user.application.port

import com.kdongsu5509.user.service.dto.AlertResponse

interface TermAlertPort {
    fun sendAlert(alertResponse: AlertResponse)
}
