package com.kdongsu5509

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.resilience.annotation.EnableResilientMethods
import java.util.*

@SpringBootApplication(scanBasePackages = ["com.kdongsu5509"])
@EnableResilientMethods
class ImhereUserServiceApplication

@PostConstruct
fun started() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
}

fun main(args: Array<String>) {
    runApplication<ImhereUserServiceApplication>(*args)
}
