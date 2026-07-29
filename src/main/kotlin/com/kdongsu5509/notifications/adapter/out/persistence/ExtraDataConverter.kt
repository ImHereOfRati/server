package com.kdongsu5509.notifications.adapter.out.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * 알림의 추가 데이터(FCM data 맵)를 한 컬럼에 JSON 문자열로 담는다.
 *
 * 키가 알림 종류마다 달라 별도 테이블로 정규화할 만한 구조가 아니다.
 * 컬럼 타입은 `TEXT`가 아니라 `VARCHAR`를 쓴다 — `ddl-auto: validate`는 길이 차이는 넘어가지만
 * 타입이 다르면 기동을 막기 때문에, 엔티티가 기대하는 varchar와 실제 스키마를 맞춰 둔다.
 */
@Converter
class ExtraDataConverter : AttributeConverter<Map<String, String>, String> {

    override fun convertToDatabaseColumn(attribute: Map<String, String>?): String =
        if (attribute.isNullOrEmpty()) EMPTY_JSON else MAPPER.writeValueAsString(attribute)

    @Suppress("UNCHECKED_CAST")
    override fun convertToEntityAttribute(dbData: String?): Map<String, String> =
        if (dbData.isNullOrBlank()) emptyMap()
        else MAPPER.readValue(dbData, Map::class.java) as Map<String, String>

    private companion object {
        const val EMPTY_JSON = "{}"

        val MAPPER: JsonMapper = JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .build()
    }
}
