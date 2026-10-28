package com.virtualca.repository;

import com.virtualca.entity.AiFinancialInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiFinancialInsightRepository extends JpaRepository<AiFinancialInsight, UUID> {
    List<AiFinancialInsight> findAllByOrderByCreatedAtDesc();

    @Query("SELECT i FROM AiFinancialInsight i WHERE (:orgId IS NULL OR i.orgId = :orgId OR i.orgId IS NULL) ORDER BY i.createdAt DESC")
    List<AiFinancialInsight> findForOrgOrGlobal(@Param("orgId") UUID orgId);
}

