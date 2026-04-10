package com.kdongsu5509.user.application.port.out.user

import java.time.Duration

interface CachePort {
    fun save(key: String, data: Any, duration: Duration)
    fun <T> find(key: String, clazz: Class<T>): T?
    fun delete(key: String)
}
