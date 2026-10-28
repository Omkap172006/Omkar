package com.virtualca.repository;

import com.virtualca.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    List<Voucher> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
    List<Voucher> findAllByOrderByCreatedAtDesc();
    long countByOrgId(UUID orgId);

    @Query("SELECT COALESCE(SUM(v.taxableAmount), 0) FROM Voucher v WHERE v.voucherType IN ('SALE', 'SALES') AND (:orgId IS NULL OR v.orgId = :orgId)")
    BigDecimal sumSalesRevenue(@Param("orgId") UUID orgId);

    @Query("SELECT COALESCE(SUM(v.taxableAmount), 0) FROM Voucher v WHERE v.voucherType IN ('PURCHASE', 'PURCHASES', 'EXPENSE') AND (:orgId IS NULL OR v.orgId = :orgId)")
    BigDecimal sumExpenses(@Param("orgId") UUID orgId);

    @Query("SELECT COALESCE(SUM(v.taxAmount), 0) FROM Voucher v WHERE v.voucherType IN ('SALE', 'SALES') AND (:orgId IS NULL OR v.orgId = :orgId)")
    BigDecimal sumOutputGst(@Param("orgId") UUID orgId);

    @Query("SELECT COALESCE(SUM(v.taxAmount), 0) FROM Voucher v WHERE v.voucherType IN ('PURCHASE', 'PURCHASES') AND (:orgId IS NULL OR v.orgId = :orgId)")
    BigDecimal sumInputGst(@Param("orgId") UUID orgId);
}

