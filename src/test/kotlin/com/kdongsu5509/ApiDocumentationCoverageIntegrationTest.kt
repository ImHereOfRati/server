package com.kdongsu5509

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.epages.restdocs.apispec.ResourceSnippetParameters
import org.junit.jupiter.api.Test
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Executes every API mapping at least once so the OpenAPI document cannot
 * silently lose an endpoint when a feature test is refactored.
 *
 * These requests intentionally use the unauthenticated boundary. Feature
 * integration tests document successful payloads and domain behaviour; this
 * test documents the route/security contract without external dependencies.
 */
class ApiDocumentationCoverageIntegrationTest : WebIntegrationTestSupport() {

    private val id = UUID.randomUUID().toString()
    private val targetId = UUID.randomUUID().toString()
    private val email = "coverage@example.com"

    @Test
    fun every_api_mapping_has_a_documented_operation() {
        documented("admin-friend-request-delete", delete("/api/admin/friend-requests/{id}", id))
        documented("admin-friend-restriction-delete", delete("/api/admin/friend-restrictions/{id}", id))
        documented("admin-friendship-delete", delete("/api/admin/friendships/{id}", id))
        documented("admin-user-delete", delete("/api/admin/users/{email}", email))
        documented("admin-user-block", post("/api/admin/users/{email}/block", email))
        documented("admin-user-unblock", delete("/api/admin/users/{email}/block", email))
    documented("admin-user-token-delete", delete("/api/admin/users/{email}/token", email))
    documented("admin-failed-notifications", get("/api/admin/failed-notifications"))
    documented("admin-failed-notification-detail", get("/api/admin/failed-notifications/{id}", id))
    documented("admin-failed-notification-delete", delete("/api/admin/failed-notifications/{id}", id))
    documented("admin-failed-notification-redelivery", post("/api/admin/failed-notifications/redelivery-jobs"))
        documented("admin-failed-notification-redelivery-detail", post("/api/admin/failed-notifications/{id}/redelivery-jobs", id))
    documented("admin-friend-requests", get("/api/admin/friend-requests"))
    documented("admin-friendships", get("/api/admin/friendships"))
    documented("admin-friend-restrictions", get("/api/admin/friend-restrictions"))
    documented("agreements-read", get("/api/agreements"))
    documented("agreements-create", post("/api/agreements"))
    documented("agreements-renew", post("/api/agreements/renewals/{termId}", id))
    documented("agreements-delete", delete("/api/agreements/{termId}", id))
    documented("friend-request-delete", delete("/api/friends/requests/{id}", id))
    documented("friend-requests", get("/api/friends/requests"))
    documented("friend-request-create", post("/api/friends/requests"))
    documented("friend-request-detail", get("/api/friends/requests/{id}", id))
        documented("friend-request-sent-cancel", delete("/api/friends/requests/{id}/sent", id))
        documented("friend-request-accept", post("/api/friends/requests/{id}/accept", id))
        documented("friend-request-reject", post("/api/friends/requests/{relationId}/reject", id))
    documented("friend-restriction-status", get("/api/friends/restrictions/target/{targetUserId}", targetId))
    documented("friend-restrictions", get("/api/friends/restrictions"))
    documented("friend-restriction-create", post("/api/friends/restrictions"))
    documented("friend-restriction-unblock", delete("/api/friends/restrictions/blocked-users/{targetUserId}", targetId))
        documented("friendship-status", get("/api/friendships/target/{targetUserId}", targetId))
        documented("friendship-detail", get("/api/friendships/{id}", id))
    documented("friendship-delete", delete("/api/friendships/{id}", id))
    documented("friendships", get("/api/friendships"))
        documented("friendship-alias", patch("/api/friendships/{id}/alias", id))
        documented("user-withdrawal", delete("/api/users/my/withdrawal"))
        documented("map-geocode", get("/api/maps/geocode").param("query", "seoul"))
        documented("map-reverse-geocode", get("/api/maps/reverse-geocode").param("latitude", "37.5665").param("longitude", "126.9780"))
        documented("map-local-search", get("/api/maps/local-search").param("query", "cafe").param("display", "5"))
    documented("notifications-read", patch("/api/notifications/{id}/read", 1L))
    documented("notifications", get("/api/notifications"))
    }

    private fun documented(identifier: String, request: org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder) {
        documented(identifier, request, status().isUnauthorized)
    }

    private fun documented(
        identifier: String,
        request: org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder,
        expectedStatus: ResultMatcher,
    ) {
        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andDo(documentation(identifier))
    }

    private fun documentation(identifier: String): RestDocumentationResultHandler =
        MockMvcRestDocumentationWrapper.document(
            identifier,
            ResourceSnippetParameters.builder()
                .description("API 경로, 인증 요구사항, 정상 처리와 실패 처리 계약")
        )
}
