package com.kdongsu5509.terms.adapter.out

import com.kdongsu5509.terms.domain.TermTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SpringDataTermRepository : JpaRepository<TermJpaEntity, Long> {
    @Query(
        """                                                              
            SELECT t FROM TermJpaEntity t                                    
            WHERE t.type = :type                                             
            ORDER BY t.version DESC                                          
            LIMIT 1                                                          
            """
    )
    fun findLatestByType(type: TermTypes): TermJpaEntity?

    @Query(
        """
        SELECT t FROM TermJpaEntity t 
        WHERE t.id IN (
            SELECT MAX(t2.id) 
            FROM TermJpaEntity t2 
            WHERE t2.effectiveDate <= CURRENT_TIMESTAMP 
            GROUP BY t2.type
        )
    """
    )
    fun findActiveAll(): List<TermJpaEntity>
}
