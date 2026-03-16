package com.kdongsu5509.notifications.adapter.in;

@Slf4j
@RestController("/api/v1/noti")
public class FcmTokenEnrollController {

    @PostMapping("/enroll")
    void enroll(@Validated @RequestBody FcmTokenInfo fcmTokenInfo) {

    }
}
