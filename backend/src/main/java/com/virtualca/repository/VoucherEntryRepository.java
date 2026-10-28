package com.virtualca.repository;

import com.virtualca.entity.VoucherEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VoucherEntryRepository extends JpaRepository<VoucherEntry, UUID> {
    List<VoucherEntry> findByVoucherId(UUID voucherId);
}

