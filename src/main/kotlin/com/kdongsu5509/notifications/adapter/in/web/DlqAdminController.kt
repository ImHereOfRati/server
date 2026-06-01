package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.notifications.adapter.`in`.web.dto.DlqQueueInfoResponse
import com.kdongsu5509.notifications.adapter.`in`.web.dto.DlqReplayResponse
import com.kdongsu5509.notifications.application.service.DlqAdminService
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toOkResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/dead-letter-queues", version = "1")
class DlqAdminController(
    private val dlqAdminService: DlqAdminService
) {
    /** 전체 DLQ 목록 및 메시지 수 조회 */
    @GetMapping
    fun getAllDlqInfo(): ResponseEntity<ApiResponse<List<DlqQueueInfoResponse>>> =
        dlqAdminService.getAllDlqInfo().toOkResponse()

    /** 특정 DLQ 정보 조회 */
    @GetMapping("/{queueName}")
    fun getDlqInfo(@PathVariable queueName: String): ResponseEntity<ApiResponse<DlqQueueInfoResponse>> =
        dlqAdminService.getQueueInfo(queueName).toOkResponse()

    /**
     * DLQ 메시지 재처리
     * @param count 재처리할 최대 메시지 수. 생략하면 전체 재처리
     */
    @PostMapping("/{queueName}/replay-jobs")
    fun replayMessages(
        @PathVariable queueName: String,
        @RequestParam(required = false) count: Int?
    ): ResponseEntity<ApiResponse<DlqReplayResponse>> =
        dlqAdminService.replayMessages(queueName, count).toOkResponse()

    /** DLQ 전체 메시지 삭제 */
    @DeleteMapping("/{queueName}/messages")
    fun purgeQueue(@PathVariable queueName: String): ResponseEntity<ApiResponse<Unit>> {
        dlqAdminService.purgeQueue(queueName)
        return Unit.toOkResponse()
    }
}
