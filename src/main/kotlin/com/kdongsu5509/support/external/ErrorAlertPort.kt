package com.kdongsu5509.support.external

interface ErrorAlertPort {
    fun send(channel: AlertChannel, message: AlertMessage)
}

enum class AlertChannel {
    CLIENT_ERROR,

    SERVER_ERROR,

}
