package com.kdongsu5509.maps

object NaverMapResponseSanitizer {
    fun geocode(raw: Map<String, Any?>): Map<String, Any?> =
        mapOf(
            "addresses" to raw.listOfMaps("addresses").map {
                it.only(
                    "roadAddress",
                    "jibunAddress",
                    "englishAddress",
                    "x",
                    "y",
                    "distance",
                )
            },
        )

    fun reverseGeocode(raw: Map<String, Any?>): Map<String, Any?> =
        mapOf(
            "results" to raw.listOfMaps("results").map {
                mapOf(
                    "name" to it["name"],
                    "region" to (it["region"] as? Map<*, *>)?.sanitisedRegion(),
                    "land" to (it["land"] as? Map<*, *>)?.only(
                        "type",
                        "name",
                        "number1",
                        "number2",
                        "addition0",
                        "addition1",
                        "addition2",
                        "addition3",
                        "addition4",
                    ),
                )
            },
        )

    fun localSearch(raw: Map<String, Any?>): Map<String, Any?> =
        mapOf(
            "items" to raw.listOfMaps("items").map {
                it.only(
                    "title",
                    "link",
                    "category",
                    "description",
                    "telephone",
                    "address",
                    "roadAddress",
                    "mapx",
                    "mapy",
                )
            },
        )

    private fun Map<String, Any?>.listOfMaps(key: String): List<Map<String, Any?>> =
        (this[key] as? List<*>).orEmpty().mapNotNull { value ->
            (value as? Map<*, *>)?.entries?.associate {
                it.key.toString() to it.value
            }
        }

    private fun Map<*, *>.only(vararg keys: String): Map<String, Any?> =
        keys.associateWith { this[it] }.filterValues { it != null }

    private fun Map<*, *>.sanitisedRegion(): Map<String, Any?> =
        listOf("area0", "area1", "area2", "area3", "area4")
            .associateWith { area ->
                (this[area] as? Map<*, *>)?.only("name", "coords")
            }
            .filterValues { it != null }
}
