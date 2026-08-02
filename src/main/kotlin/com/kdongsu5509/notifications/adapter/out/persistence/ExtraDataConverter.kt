package com.kdongsu5509.notifications.adapter.out.persistence

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

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
