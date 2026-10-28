package com.virtualca.repository;

import com.virtualca.entity.ComplianceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplianceEventRepository extends JpaRepository<ComplianceEvent, UUID> {
    List<ComplianceEvent> findAllByOrderByDueDateAsc();

    @Query("SELECT c FROM ComplianceEvent c WHERE (:orgId IS NULL OR c.orgId = :orgId OR c.orgId IS NULL) ORDER BY c.dueDate ASC")
    List<ComplianceEvent> findForOrgOrGlobal(@Param("orgId") UUID orgId);
}

