package com.kdongsu5509.support.external

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class UserErrorAlertNotifier(
    private val errorAlertPort: ErrorAlertPort,
) {

    fun notifyUserError(request: HttpServletRequest, errorCode: String, errorMessage: String) {
        errorAlertPort.send(
            AlertChannel.CLIENT_ERROR,
            AlertMessage.userError(errorCode, errorMessage, RequestContext.from(request)),
        )
    }

    fun notifyAbnormalAccess(request: HttpServletRequest, errorCode: String, errorMessage: String) {
        errorAlertPort.send(
            AlertChannel.CLIENT_ERROR,
            AlertMessage.abnormalAccess(errorCode, errorMessage, RequestContext.from(request)),
        )
    }
}
