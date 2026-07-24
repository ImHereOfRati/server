package com.kdongsu5509.agreement.domain

data class Consent(val items: List<ConsentItem>) {
    fun changes(currentStatus: Map<Long, AgreementStatus>): List<ConsentChange> {
        val uniqueItems = items.associateBy { it.termId }
        val changes = mutableListOf<ConsentChange>()

        for (item in uniqueItems.values) {
            val current = currentStatus[item.termId]
            val next = AgreementStatus.next(current, item.isAgreed)

            if (next != null) {
                changes.add(ConsentChange(item.termId, next))
            }
        }

        return changes
    }
}
