package com.virtualca.service;

import com.virtualca.entity.AiFinancialInsight;
import com.virtualca.repository.AiFinancialInsightRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InsightService {

    private final AiFinancialInsightRepository aiFinancialInsightRepository;

    public InsightService(AiFinancialInsightRepository aiFinancialInsightRepository) {
        this.aiFinancialInsightRepository = aiFinancialInsightRepository;
    }

    public List<AiFinancialInsight> getAiInsights(UUID orgId) {
        return aiFinancialInsightRepository.findForOrgOrGlobal(orgId);
    }
}

