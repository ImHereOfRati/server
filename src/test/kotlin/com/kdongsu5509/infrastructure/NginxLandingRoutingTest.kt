package com.kdongsu5509.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class NginxLandingRoutingTest {
    private val repositoryRoot: Path = Path.of("").toAbsolutePath()
    private val nginxTemplate: Path = repositoryRoot.resolve("infra/nginx/nginx.conf.template")
    private val composeFile: Path = repositoryRoot.resolve("docker-compose.yml")
    private val legacyLanding: Path = repositoryRoot.resolve("infra/nginx/website.html")

    @Test
    @DisplayName("백엔드 경로는 프록시하고 그 외 HTTPS 요청은 신규 랜딩으로 이동한다")
    fun routing_success_preserves_backend_locations_before_landing_redirect() {
        // given
        val nginx = Files.readString(nginxTemplate)
        val catchAllIndex = nginx.lastIndexOf("location / {")

        // when
        val backendLocations = listOf(
            "location ^~ /api/",
            "location ^~ /admin/",
            "location ^~ /swagger-ui/",
            "location ^~ /docs/",
            "location ^~ \${MGMT_BASE_PATH}/",
        )

        // then
        assertThat(backendLocations)
            .allSatisfy { location ->
                assertThat(nginx.indexOf(location))
                    .isGreaterThanOrEqualTo(0)
                    .isLessThan(catchAllIndex)
            }
        assertThat(nginx)
            .contains("location = / {")
            .contains("return 301 https://imhere.ratiko.co.kr\$request_uri;")
            .doesNotContain("try_files")
    }

    @Test
    @DisplayName("기존 nginx 랜딩 파일과 compose 마운트를 제거한다")
    fun compose_success_removes_legacy_landing_mount() {
        // given
        val compose = Files.readString(composeFile)

        // when & then
        assertThat(Files.exists(legacyLanding)).isFalse()
        assertThat(compose).doesNotContain("website.html")
        assertThat(compose).doesNotContain("/usr/share/nginx/html/index.html")
    }
}
