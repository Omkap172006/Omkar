package com.virtualca.service;

import com.virtualca.entity.ComplianceEvent;
import com.virtualca.repository.ComplianceEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ComplianceService {

    private final ComplianceEventRepository complianceEventRepository;

    public ComplianceService(ComplianceEventRepository complianceEventRepository) {
        this.complianceEventRepository = complianceEventRepository;
    }

    public List<ComplianceEvent> getComplianceEvents(UUID orgId) {
        return complianceEventRepository.findForOrgOrGlobal(orgId);
    }
}

